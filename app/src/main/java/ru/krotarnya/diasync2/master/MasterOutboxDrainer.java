package ru.krotarnya.diasync2.master;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import ru.krotarnya.diasync2.common.master.XdripEvent;
import ru.krotarnya.diasync2.common.master.XdripEventCodec;
import ru.krotarnya.diasync2.data.api.ApiDataPointDto;
import ru.krotarnya.diasync2.data.api.MasterUploadDataSource;
import ru.krotarnya.diasync2.data.api.MasterUploadHttpException;
import ru.krotarnya.diasync2.data.api.MasterUploadParseException;
import ru.krotarnya.diasync2.data.local.MasterEventDao;
import ru.krotarnya.diasync2.data.local.MasterEventEntity;
import ru.krotarnya.diasync2.settings.AppConfiguration;

public final class MasterOutboxDrainer implements MasterUploadWork {
    public static final int MAX_BATCH_SIZE = 200;
    public static final Duration LEASE_DURATION = Duration.ofMinutes(2);

    public enum Kind {
        IDLE,
        DELIVERED,
        RETRY_SCHEDULED,
        BLOCKED
    }

    public record Result(Kind kind, int eventCount, String diagnosticCode) {
        static Result idle() {
            return new Result(Kind.IDLE, 0, null);
        }
    }

    private final MasterEventDao dao;
    private final MasterUploadDataSource dataSource;
    private final XdripEventCodec codec;
    private final Clock clock;
    private final Function<Integer, Duration> retryDelay;

    public MasterOutboxDrainer(
            MasterEventDao dao,
            MasterUploadDataSource dataSource,
            XdripEventCodec codec,
            Clock clock,
            Function<Integer, Duration> retryDelay
    ) {
        this.dao = Objects.requireNonNull(dao);
        this.dataSource = Objects.requireNonNull(dataSource);
        this.codec = Objects.requireNonNull(codec);
        this.clock = Objects.requireNonNull(clock);
        this.retryDelay = Objects.requireNonNull(retryDelay);
    }

    @Override
    public synchronized Result drainOnce(
            AppConfiguration configuration,
            Runnable onUploadStarted
    ) {
        Objects.requireNonNull(configuration);
        Objects.requireNonNull(onUploadStarted);
        Instant now = clock.instant();
        String leaseUntil = now.plus(LEASE_DURATION).toString();
        List<MasterEventEntity> batch = dao.claimEligible(
                MasterEventRepository.destinationFingerprint(configuration),
                now.toString(),
                leaseUntil,
                MAX_BATCH_SIZE);
        if (batch.isEmpty()) {
            int retryable = dao.countRetryableEvents(
                    MasterEventRepository.destinationFingerprint(configuration));
            if (retryable > 0) {
                return new Result(Kind.RETRY_SCHEDULED, retryable, null);
            }
            return dao.countUndeliveredEvents() > 0
                    ? new Result(Kind.BLOCKED, 0, null)
                    : Result.idle();
        }
        onUploadStarted.run();
        try {
            return upload(configuration, batch, leaseUntil);
        } catch (IOException exception) {
            return retry(batch, leaseUntil, "NETWORK_ERROR");
        } catch (MasterUploadParseException | InvalidAcknowledgementException exception) {
            return retry(batch, leaseUntil, "INVALID_RESPONSE");
        } catch (RuntimeException exception) {
            return retry(batch, leaseUntil, "UPLOAD_FAILED");
        }
    }

    @Override
    public void cancelActiveCall() {
        dataSource.cancelActiveCall();
    }

    public int retryBlocked(AppConfiguration configuration) {
        return dao.retryBlocked(MasterEventRepository.destinationFingerprint(configuration));
    }

    private Result upload(
            AppConfiguration configuration,
            List<MasterEventEntity> batch,
            String leaseUntil
    ) throws IOException, MasterUploadParseException, InvalidAcknowledgementException {
        List<XdripEvent> events = decode(batch);
        List<ApiDataPointDto> request = events.stream()
                .map(event -> toUploadPoint(event, configuration.userId()))
                .collect(Collectors.toList());
        try {
            List<ApiDataPointDto> response = dataSource.addDataPoints(
                    configuration.baseUrl(),
                    request);
            List<ApiDataPointDto> acknowledgements = matchAcknowledgements(
                    events,
                    configuration.userId(),
                    response);
            Instant deliveredAt = clock.instant();
            List<MasterEventDao.DeliveryAck> deliveryAcks = new ArrayList<>(batch.size());
            for (int index = 0; index < batch.size(); index++) {
                MasterEventEntity item = batch.get(index);
                ApiDataPointDto ack = acknowledgements.get(index);
                deliveryAcks.add(new MasterEventDao.DeliveryAck(
                        item.eventId,
                        item.occurredAt,
                        ack.id,
                        Instant.parse(ack.updateTimestamp).toString()));
            }
            int delivered = dao.reconcileDeliveredBatch(
                    deliveryAcks,
                    leaseUntil,
                    deliveredAt.toString(),
                    configuration.userId());
            return new Result(Kind.DELIVERED, delivered, "UPLOAD_SUCCEEDED");
        } catch (MasterUploadHttpException exception) {
            int status = exception.statusCode();
            if (status == 413 && batch.size() > 1) {
                int middle = batch.size() / 2;
                Result first = upload(configuration, batch.subList(0, middle), leaseUntil);
                Result second = upload(configuration, batch.subList(middle, batch.size()), leaseUntil);
                return combine(first, second);
            }
            if (status >= 500 || status == 408 || status == 429) {
                return retry(batch, leaseUntil, "HTTP_RETRYABLE");
            }
            return block(batch, leaseUntil, status == 413
                    ? "EVENT_TOO_LARGE"
                    : "HTTP_REJECTED");
        }
    }

    private Result combine(Result first, Result second) {
        int count = first.eventCount + second.eventCount;
        if (first.kind == Kind.BLOCKED || second.kind == Kind.BLOCKED) {
            return new Result(Kind.BLOCKED, count, "PARTIAL_BATCH_BLOCKED");
        }
        if (first.kind == Kind.RETRY_SCHEDULED || second.kind == Kind.RETRY_SCHEDULED) {
            return new Result(Kind.RETRY_SCHEDULED, count, "PARTIAL_BATCH_RETRY");
        }
        return new Result(Kind.DELIVERED, count, "UPLOAD_SUCCEEDED");
    }

    private Result retry(
            List<MasterEventEntity> batch,
            String leaseUntil,
            String errorCode
    ) {
        int highestAttempt = batch.stream().mapToInt(event -> event.attemptCount).max().orElse(1);
        Instant nextAttempt = clock.instant().plus(retryDelay.apply(highestAttempt));
        int changed = dao.markRetry(eventIds(batch), leaseUntil, nextAttempt.toString(), errorCode);
        return new Result(Kind.RETRY_SCHEDULED, changed, errorCode);
    }

    private Result block(
            List<MasterEventEntity> batch,
            String leaseUntil,
            String errorCode
    ) {
        int changed = dao.markBlocked(eventIds(batch), leaseUntil, errorCode);
        return new Result(Kind.BLOCKED, changed, errorCode);
    }

    private List<XdripEvent> decode(List<MasterEventEntity> batch) {
        return batch.stream()
                .map(event -> codec.decode(event.payloadJson.getBytes(StandardCharsets.UTF_8)))
                .collect(Collectors.toList());
    }

    private static List<String> eventIds(List<MasterEventEntity> events) {
        return events.stream()
                .map(event -> event.eventId)
                .collect(Collectors.toList());
    }

    private static ApiDataPointDto toUploadPoint(XdripEvent event, String userId) {
        ApiDataPointDto point = new ApiDataPointDto();
        point.userId = userId;
        point.timestamp = Instant.ofEpochMilli(event.occurredAtEpochMillis()).toString();
        if (event.sensor() != null) {
            point.sensorGlucose = new ApiDataPointDto.SensorGlucoseDto();
            point.sensorGlucose.mgdl = event.sensor().rawValue();
            point.sensorGlucose.sensorId = event.sensor().sensorId();
            point.sensorGlucose.calibration = new ApiDataPointDto.CalibrationDto();
            point.sensorGlucose.calibration.slope = event.sensor().calibration().slope();
            point.sensorGlucose.calibration.intercept = event.sensor().calibration().intercept();
        } else if (event.manualGlucose() != null) {
            point.manualGlucose = new ApiDataPointDto.ManualGlucoseDto();
            point.manualGlucose.mgdl = event.manualGlucose().mgdl();
        } else {
            point.carbs = new ApiDataPointDto.CarbsDto();
            point.carbs.grams = event.carbs().grams();
            point.carbs.description = event.carbs().description();
        }
        return point;
    }

    private static List<ApiDataPointDto> matchAcknowledgements(
            List<XdripEvent> events,
            String userId,
            List<ApiDataPointDto> response
    ) throws InvalidAcknowledgementException {
        if (response.size() != events.size()) {
            throw new InvalidAcknowledgementException();
        }
        List<ApiDataPointDto> unmatched = new ArrayList<>(response);
        List<ApiDataPointDto> matched = new ArrayList<>(events.size());
        for (XdripEvent event : events) {
            int match = -1;
            for (int index = 0; index < unmatched.size(); index++) {
                if (acknowledges(event, userId, unmatched.get(index))) {
                    match = index;
                    break;
                }
            }
            if (match < 0) {
                throw new InvalidAcknowledgementException();
            }
            matched.add(unmatched.remove(match));
        }
        return matched;
    }

    private static boolean acknowledges(XdripEvent event, String userId, ApiDataPointDto ack) {
        if (ack == null || ack.id == null || ack.updateTimestamp == null
                || !userId.equals(ack.userId) || ack.timestamp == null) {
            return false;
        }
        try {
            if (!Instant.ofEpochMilli(event.occurredAtEpochMillis()).equals(
                    Instant.parse(ack.timestamp))) {
                return false;
            }
            Instant.parse(ack.updateTimestamp);
        } catch (RuntimeException exception) {
            return false;
        }
        if (event.sensor() != null) {
            return ack.sensorGlucose != null
                    && equal(event.sensor().rawValue(), ack.sensorGlucose.mgdl)
                    && event.sensor().sensorId().equals(ack.sensorGlucose.sensorId)
                    && ack.sensorGlucose.calibration != null
                    && equal(event.sensor().calibration().slope(),
                            ack.sensorGlucose.calibration.slope)
                    && equal(event.sensor().calibration().intercept(),
                            ack.sensorGlucose.calibration.intercept);
        }
        if (event.manualGlucose() != null) {
            return ack.manualGlucose != null
                    && equal(event.manualGlucose().mgdl(), ack.manualGlucose.mgdl);
        }
        return ack.carbs != null
                && equal(event.carbs().grams(), ack.carbs.grams)
                && Objects.equals(event.carbs().description(), ack.carbs.description);
    }

    private static boolean equal(double expected, Double actual) {
        return actual != null && Double.compare(expected, actual) == 0;
    }

    private static final class InvalidAcknowledgementException extends Exception {
    }
}
