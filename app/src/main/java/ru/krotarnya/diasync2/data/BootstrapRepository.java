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
            List<ApiDataPointDto> response = dataSource.getDataPoints(baseUrl, userId, from, to);
            List<DataPointEntity> entities = new ArrayList<>(response.size());
            for (ApiDataPointDto dto : response) {
                entities.add(mapper.toEntity(dto, userId));
            }
            dao.applyBootstrap(
                    entities,
                    new SyncStateEntity(userId, null, to.toString(), null));
            if (entities.isEmpty()) {
                return BootstrapResult.noData();
            }
            DataPointEntity latest = dao.latestSensorPoint(userId);
            return latest == null
                    ? BootstrapResult.noData()
                    : BootstrapResult.success(mapper.toDomain(latest));
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
}
