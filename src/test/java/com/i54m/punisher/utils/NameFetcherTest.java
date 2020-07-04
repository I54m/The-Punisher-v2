package com.i54m.punisher.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NameFetcherTest {

    @Test
    void getName() {
        assertEquals("54mpenguin", NameFetcher.getName("74f04a9bb7f9409da940b051f14dd3a5"));
    }

    @Test
    void getStoredName() {
        NameFetcher.storeName("74f04a9bb7f9409da940b051f14dd3a5", "54mpenguin");
        assertEquals("54mpenguin", NameFetcher.getName("74f04a9bb7f9409da940b051f14dd3a5"));
    }
}