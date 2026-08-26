package ru.krotarnya.diasync2.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class Java17CompatibilityTest {
    @Test
    public void supportsJava17LanguageFeatures() {
        record Fixture(int value) {}

        assertEquals(17, new Fixture(17).value());
    }
}
