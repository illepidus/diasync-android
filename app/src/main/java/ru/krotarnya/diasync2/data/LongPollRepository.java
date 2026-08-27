package ru.krotarnya.diasync2.data;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import ru.krotarnya.diasync2.data.api.ApiDataPointDto;
import ru.krotarnya.diasync2.data.api.BootstrapHttpException;
import ru.krotarnya.diasync2.data.api.BootstrapParseException;
import ru.krotarnya.diasync2.data.api.LongPollCall;
import ru.krotarnya.diasync2.data.api.LongPollDataSource;
import ru.krotarnya.diasync2.data.local.BootstrapDao;
import ru.krotarnya.diasync2.data.local.DataPointEntity;
import ru.krotarnya.diasync2.data.local.SyncStateEntity;

public final class LongPollRepository {
    private final LongPollDataSource dataSource;
    private final BootstrapDao dao;
    private final DataPointMapper mapper;
    private final Clock clock;
    private final AtomicReference<LongPollCall> activeCall = new AtomicReference<>();

    public LongPollRepository(
            LongPollDataSource dataSource,
            BootstrapDao dao,
            DataPointMapper mapper,
            Clock clock
    ) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.dao = Objects.requireNonNull(dao);
        this.mapper = Objects.requireNonNull(mapper);
        this.clock = Objects.requireNonNull(clock);
    }

    public LongPollResult poll(String baseUrl, String userId, Instant since) {
        LongPollCall call;
        try {
            call = dataSource.newCall(baseUrl, userId, since);
        } catch (IllegalArgumentException exception) {
            return LongPollResult.of(LongPollResult.Kind.INVALID_DATA);
        }
        activeCall.set(call);
        try {
            List<ApiDataPointDto> response = call.execute();
            if (response.isEmpty()) {
                dao.recordEmptyPollSuccess(userId, clock.instant().toString());
                return LongPollResult.of(LongPollResult.Kind.EMPTY);
            }
            List<DataPointEntity> entities = new ArrayList<>(response.size());
            Instant maximumCursor = null;
            for (ApiDataPointDto dto : response) {
                DataPointEntity entity = mapper.toEntity(dto, userId);
                if (entity.updateTimestamp == null) {
                    return LongPollResult.of(LongPollResult.Kind.INVALID_DATA);
                }
                Instant updateTimestamp = Instant.parse(entity.updateTimestamp);
                if (maximumCursor == null || updateTimestamp.isAfter(maximumCursor)) {
                    maximumCursor = updateTimestamp;
                }
                entities.add(entity);
            }
            dao.applyLongPollBatch(
                    entities,
                    new SyncStateEntity(
                            userId,
                            maximumCursor.toString(),
                            clock.instant().toString(),
                            null));
            return LongPollResult.data(maximumCursor);
        } catch (BootstrapHttpException exception) {
            return LongPollResult.of(LongPollResult.Kind.HTTP_ERROR);
        } catch (BootstrapParseException exception) {
            return LongPollResult.of(LongPollResult.Kind.PARSE_ERROR);
        } catch (IOException exception) {
            return LongPollResult.of(call == activeCall.get() && !Thread.currentThread().isInterrupted()
                    ? LongPollResult.Kind.CONNECTION_ERROR
                    : LongPollResult.Kind.CANCELLED);
        } catch (IllegalArgumentException exception) {
            return LongPollResult.of(LongPollResult.Kind.INVALID_DATA);
        } catch (RuntimeException exception) {
            return LongPollResult.of(LongPollResult.Kind.STORAGE_ERROR);
        } finally {
            activeCall.compareAndSet(call, null);
        }
    }

    public void cancelActiveCall() {
        LongPollCall call = activeCall.getAndSet(null);
        if (call != null) {
            call.cancel();
        }
    }
}
