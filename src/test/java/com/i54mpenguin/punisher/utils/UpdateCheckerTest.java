package com.i54mpenguin.punisher.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UpdateCheckerTest {

    @Test
    void getCurrentVersionNumber() {
        assertNotNull(UpdateChecker.getCurrentVersion());
    }

    @Test
    void getLastUpdate() {
        assertNotNull(UpdateChecker.getRealeaseDate());
    }

    @Test
    void check() {
        assertTrue(UpdateChecker.check("1.0"));
        assertFalse(UpdateChecker.check("9999.0"));
    }
}