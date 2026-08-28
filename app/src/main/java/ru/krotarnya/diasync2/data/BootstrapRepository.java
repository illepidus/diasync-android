package ru.krotarnya.diasync2.data;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ru.krotarnya.diasync2.common.DataPoint;
import ru.krotarnya.diasync2.data.api.ApiDataPointDto;
import ru.krotarnya.diasync2.data.api.BootstrapDataSource;
import ru.krotarnya.diasync2.data.api.BootstrapHttpException;
import ru.krotarnya.diasync2.data.api.BootstrapParseException;
import ru.krotarnya.diasync2.data.local.BootstrapDao;
import ru.krotarnya.diasync2.data.local.DataPointEntity;
import ru.krotarnya.diasync2.data.local.SyncStateEntity;

public final class BootstrapRepository {
    public static final Duration BOOTSTRAP_WINDOW = Duration.ofHours(4);
    public static final Duration LONG_POLL_OVERLAP = Duration.ofMinutes(1);

    private final BootstrapDataSource dataSource;
    private final BootstrapDao dao;
    private final DataPointMapper mapper;
    private final Clock clock;

    public BootstrapRepository(
            BootstrapDataSource dataSource,
            BootstrapDao dao,
            DataPointMapper mapper,
            Clock clock
    ) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.dao = Objects.requireNonNull(dao);
        this.mapper = Objects.requireNonNull(mapper);
        this.clock = Objects.requireNonNull(clock);
    }

    public BootstrapResult bootstrap(String baseUrl, String userId) {
        Instant to = clock.instant();
        Instant from = to.minus(BOOTSTRAP_WINDOW);
        try {
            SyncStateEntity previousState = dao.syncState(userId);
            List<ApiDataPointDto> response = dataSource.getDataPoints(baseUrl, userId, from, to);
            List<DataPointEntity> entities = new ArrayList<>(response.size());
            Instant maximumUpdateTimestamp = null;
            for (ApiDataPointDto dto : response) {
                DataPointEntity entity = mapper.toEntity(dto, userId);
                entities.add(entity);
                if (entity.updateTimestamp != null) {
                    Instant updateTimestamp = Instant.parse(entity.updateTimestamp);
                    if (maximumUpdateTimestamp == null
                            || updateTimestamp.isAfter(maximumUpdateTimestamp)) {
                        maximumUpdateTimestamp = updateTimestamp;
                    }
                }
            }
            dao.applyBootstrap(
                    entities,
                    new SyncStateEntity(
                            userId,
                            previousState == null ? null : previousState.cursorUpdateTimestamp,
                            to.toString(),
                            null));
            Instant initialPollSince;
            if (previousState != null && previousState.cursorUpdateTimestamp != null) {
                initialPollSince = Instant.parse(previousState.cursorUpdateTimestamp);
            } else {
                initialPollSince = maximumUpdateTimestamp == null
                        ? from
                        : maximumUpdateTimestamp.minus(LONG_POLL_OVERLAP);
            }
            if (entities.isEmpty()) {
                return BootstrapResult.noData(initialPollSince);
            }
            DataPointEntity latest = dao.latestSensorPoint(userId);
            return latest == null
                    ? BootstrapResult.noData(initialPollSince)
                    : BootstrapResult.success(mapper.toDomain(latest), initialPollSince);
        } catch (BootstrapHttpException exception) {
            return BootstrapResult.error(BootstrapResult.Kind.HTTP_ERROR);
        } catch (BootstrapParseException exception) {
            return BootstrapResult.error(BootstrapResult.Kind.PARSE_ERROR);
        } catch (IOException exception) {
            return BootstrapResult.error(BootstrapResult.Kind.CONNECTION_ERROR);
        } catch (IllegalArgumentException exception) {
            return BootstrapResult.error(BootstrapResult.Kind.INVALID_DATA);
        } catch (RuntimeException exception) {
            return BootstrapResult.error(BootstrapResult.Kind.STORAGE_ERROR);
        }
    }

    public DataPoint latestLocalSensorPoint(String userId) {
        DataPointEntity entity = dao.latestSensorPoint(userId);
        return entity == null ? null : mapper.toDomain(entity);
    }

    public List<DataPoint> latestLocalSensorPoints(String userId, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        List<DataPointEntity> entities = dao.latestSensorPoints(userId, limit);
        List<DataPoint> points = new ArrayList<>(entities.size());
        for (DataPointEntity entity : entities) {
            points.add(mapper.toDomain(entity));
        }
        return points;
    }

    public List<DataPoint> latestLocalSensorPointsSince(
            String userId,
            Instant from,
            int limit
    ) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        List<DataPointEntity> entities = dao.latestSensorPoints(userId, limit);
        List<DataPoint> points = new ArrayList<>(entities.size());
        for (DataPointEntity entity : entities) {
            DataPoint point = mapper.toDomain(entity);
            if (!point.timestamp().isBefore(from)) {
                points.add(point);
            }
        }
        return points;
    }
}
