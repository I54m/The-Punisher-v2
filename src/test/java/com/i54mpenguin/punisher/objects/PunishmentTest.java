package com.i54mpenguin.punisher.objects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PunishmentTest {

    @Test
    void isPermanent() {
        Punishment punishment = new Punishment(0, Punishment.Type.ALL, Punishment.Reason.Custom, "N/A", ((long) 4.73e+11 + System.currentTimeMillis()), "N/A", "N/A", "N/A", "N/A", Punishment.Status.Active, null);
        assertTrue(punishment.isPermanent());
    }

    @Test
    void isRepBan() {
    }

    @Test
    void hasExpiration() {
    }

    @Test
    void verify() {
    }

    @Test
    void setIssueDate() {
    }

    @Test
    void setPunisherUUID() {
    }

    @Test
    void setExpiration() {
    }

    @Test
    void setMessage() {
    }

    @Test
    void setStatus() {
    }

    @Test
    void setRemoverUUID() {
    }

    @Test
    void getIssueDate() {
    }

    @Test
    void getHoverEvent() {
    }

    @Test
    void getHoverText() {
    }
}