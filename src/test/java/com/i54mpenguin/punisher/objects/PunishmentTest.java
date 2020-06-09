package com.i54mpenguin.punisher.objects;

import com.i54mpenguin.punisher.utils.NameFetcher;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PunishmentTest {

    @Test
    void isPermanent() {
        Punishment punishment = new Punishment(0, Punishment.Type.ALL, Punishment.Reason.Custom, "N/A", ((long) 4.73e+11 + System.currentTimeMillis()), "N/A", "N/A", "N/A", "N/A", Punishment.Status.Active, null);
        assertTrue(punishment.isPermanent());
    }

    @Test
    void isRepBan() {
        Punishment ban = new Punishment(0, Punishment.Type.BAN, Punishment.Reason.Custom, "N/A", (long) 3.154e+12 + System.currentTimeMillis(), "N/A", "N/A", "CONSOLE", "Overly Toxic (Rep dropped below -10)", Punishment.Status.Active, null);
        assertTrue(ban.isRepBan());
    }

    @Test
    void hasExpiration() {
        Punishment punishment = new Punishment(0, Punishment.Type.ALL, Punishment.Reason.Custom, "N/A", ((long) 4.73e+11 + System.currentTimeMillis()), "N/A", "N/A", "N/A", "N/A", Punishment.Status.Active, null);
        assertTrue(punishment.hasExpiration());
    }

    @Test
    void verify() {
        Punishment ban = new Punishment(0, Punishment.Type.BAN, Punishment.Reason.Custom, "N/A", null, "74f04a9bb7f9409da940b051f14dd3a5", null, null, null, Punishment.Status.Active, null);
        NameFetcher.storeName("74f04a9bb7f9409da940b051f14dd3a5", "54mpenguin");
        ban.verify();
        assertEquals("54mpenguin", ban.getTargetName());
        assertEquals("CONSOLE", ban.getPunisherUUID());
        assertTrue(ban.hasExpiration());
        assertTrue(ban.hasMessage());
    }

    @Test
    void setIssueDate() {
        Punishment punishment = new Punishment(0, Punishment.Type.ALL, Punishment.Reason.Custom, null, ((long) 4.73e+11 + System.currentTimeMillis()), "N/A", "N/A", "N/A", "N/A", Punishment.Status.Active, null);
        punishment.setIssueDate();
        assertEquals(DateTimeFormatter.ofPattern("dd/MMM/yy HH:mm").format(LocalDateTime.now()), punishment.getIssueDate());
    }

    @Test
    void setPunisherUUID() {
        Punishment punishment = new Punishment(0, Punishment.Type.ALL, Punishment.Reason.Custom, null, ((long) 4.73e+11 + System.currentTimeMillis()), "N/A", "N/A", "N/A", "N/A", Punishment.Status.Active, null);
        punishment.setPunisherUUID("74f04a9bb7f9409da940b051f14dd3a5");
        assertEquals("74f04a9bb7f9409da940b051f14dd3a5", punishment.getPunisherUUID());
    }

    @Test
    void setExpiration() {
        Punishment ban = new Punishment(0, Punishment.Type.BAN, Punishment.Reason.Custom, "N/A", null, "74f04a9bb7f9409da940b051f14dd3a5", null, null, null, Punishment.Status.Active, null);
        ban.setExpiration(((long) 4.73e+11 + System.currentTimeMillis()));
        assertEquals(((long) 4.73e+11 + System.currentTimeMillis()), ban.getExpiration());
    }

    @Test
    void setMessage() {
        Punishment punishment = new Punishment(0, Punishment.Type.ALL, Punishment.Reason.Custom, null, ((long) 4.73e+11 + System.currentTimeMillis()), "N/A", "N/A", "N/A", "N/A", Punishment.Status.Active, null);
        punishment.setMessage("Banned Permanently!");
        assertEquals("Banned Permanently!", punishment.getMessage());
    }

    @Test
    void setStatus() {
        Punishment punishment = new Punishment(0, Punishment.Type.ALL, Punishment.Reason.Custom, null, ((long) 4.73e+11 + System.currentTimeMillis()), "N/A", "N/A", "N/A", "N/A", Punishment.Status.Pending, null);
        punishment.setStatus(Punishment.Status.Active);
        assertEquals(Punishment.Status.Active, punishment.getStatus());
    }

    @Test
    void setRemoverUUID() {
        Punishment punishment = new Punishment(0, Punishment.Type.ALL, Punishment.Reason.Custom, null, ((long) 4.73e+11 + System.currentTimeMillis()), "N/A", "N/A", "N/A", "N/A", Punishment.Status.Pending, null);
        punishment.setRemoverUUID("74f04a9bb7f9409da940b051f14dd3a5");
        assertEquals("74f04a9bb7f9409da940b051f14dd3a5", punishment.getRemoverUUID());
    }

    @Test
    void getIssueDate() {
        Punishment punishment = new Punishment(0, Punishment.Type.ALL, Punishment.Reason.Custom, null, ((long) 4.73e+11 + System.currentTimeMillis()), "N/A", "N/A", "N/A", "N/A", Punishment.Status.Active, null);
        assertEquals(punishment.getIssueDate(), "N/A");
    }
}