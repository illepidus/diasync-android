package ru.krotarnya.diasync2.settings;

import static org.junit.Assert.assertArrayEquals;

import java.time.Duration;
import java.util.Arrays;
import org.junit.Test;

public class SnoozeOptionTest {
    @Test
    public void exposesEverySpecifiedDurationInUiOrder() {
        Duration[] actual = Arrays.stream(SnoozeOption.values())
                .map(SnoozeOption::duration)
                .toArray(Duration[]::new);

        assertArrayEquals(new Duration[]{
                Duration.ofMinutes(5),
                Duration.ofMinutes(10),
                Duration.ofMinutes(15),
                Duration.ofMinutes(20),
                Duration.ofMinutes(30),
                Duration.ofHours(1),
                Duration.ofHours(2),
                Duration.ofHours(4),
                Duration.ofHours(6),
                Duration.ofHours(8),
                Duration.ofHours(10),
                Duration.ofHours(12),
                Duration.ofHours(24)
        }, actual);
    }
}
