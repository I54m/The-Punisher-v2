package com.i54mpenguin.punisher.utils;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UUIDFetcherTest {

    @Test
    void formatUUID() {
        assertEquals(UUID.fromString("74f04a9b-b7f9-409d-a940-b051f14dd3a5"), UUIDFetcher.formatUUID("74f04a9bb7f9409da940b051f14dd3a5"));
    }

    @Test
    void call() throws Exception {
        UUIDFetcher uuidFetcher = new UUIDFetcher();
        uuidFetcher.fetch("54mpenguin");
        assertEquals("74f04a9bb7f9409da940b051f14dd3a5", uuidFetcher.call());
    }

    @Test
    void callStoredUUID() throws Exception {
        UUIDFetcher uuidFetcher = new UUIDFetcher();
        uuidFetcher.storeUUID("74f04a9bb7f9409da940b051f14dd3a5", "54mpenguin");
        uuidFetcher.fetch("54mpenguin");
        assertEquals("74f04a9bb7f9409da940b051f14dd3a5", uuidFetcher.call());
    }
}