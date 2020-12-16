package com.i54m.punisher.objects;

import com.i54m.punisher.utils.NameFetcher;
import com.i54m.punisher.utils.UUIDFetcher;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PunishmentTest {

    @Test
    void isPermanent() {
        Punishment punishment = new Punishment(0, Punishment.Type.ALL, "Custom", "N/A", ((long) 4.73e+11 + System.currentTimeMillis()), UUIDFetcher.getBLANK_UUID(), "N/A", UUIDFetcher.getBLANK_UUID(), "N/A", Punishment.Status.Active, null, null, new Punishment.MetaData());
        assertTrue(punishment.isPermanent());
    }

    @Test
    void isRepBan() {
        Punishment ban = new Punishment(0, Punishment.Type.BAN, "Custom", "N/A", (long) 3.154e+12 + System.currentTimeMillis(), UUIDFetcher.getBLANK_UUID(), "N/A", UUIDFetcher.getBLANK_UUID(), "Overly Toxic (Rep dropped below -10)", Punishment.Status.Active, null, null, new Punishment.MetaData(true, false, false, true));
        assertTrue(ban.isRepBan());
    }

    @Test
    void hasExpiration() {
        Punishment punishment = new Punishment(0, Punishment.Type.ALL, "Custom", "N/A", ((long) 4.73e+11 + System.currentTimeMillis()), UUIDFetcher.getBLANK_UUID(), "N/A", UUIDFetcher.getBLANK_UUID(), "N/A", Punishment.Status.Active, null, null, new Punishment.MetaData());
        assertTrue(punishment.hasExpiration());
    }

    @Test
    void verify() {
        Punishment ban = new Punishment(0, Punishment.Type.BAN, "Custom", "N/A", null, UUIDFetcher.formatUUID("74f04a9bb7f9409da940b051f14dd3a5"), null, null, null, Punishment.Status.Active, null, null, new Punishment.MetaData());
        NameFetcher.storeName(UUIDFetcher.formatUUID("74f04a9bb7f9409da940b051f14dd3a5"), "I54m");
        ban.verify();
        assertEquals("I54m", ban.getTargetName());
        assertEquals(UUIDFetcher.getBLANK_UUID(), ban.getPunisherUUID());
        assertTrue(ban.hasExpiration());
        assertTrue(ban.hasMessage());
    }

    @Test
    void setIssueDate() {
        Punishment punishment = new Punishment(0, Punishment.Type.ALL, "Custom", null, ((long) 4.73e+11 + System.currentTimeMillis()), UUIDFetcher.getBLANK_UUID(), "N/A", UUIDFetcher.getBLANK_UUID(), "N/A", Punishment.Status.Active, null, null, new Punishment.MetaData());
        punishment.setIssueDate();
        assertEquals(DateTimeFormatter.ofPattern("dd/MMM/yy HH:mm").format(LocalDateTime.now()), punishment.getIssueDate());
    }

    @Test
    void setPunisherUUID() {
        Punishment punishment = new Punishment(0, Punishment.Type.ALL, "Custom", null, ((long) 4.73e+11 + System.currentTimeMillis()), UUIDFetcher.getBLANK_UUID(), "N/A", UUIDFetcher.getBLANK_UUID(), "N/A", Punishment.Status.Active, null, null, new Punishment.MetaData());
        punishment.setPunisherUUID(UUIDFetcher.formatUUID("74f04a9bb7f9409da940b051f14dd3a5"));
        assertEquals(UUIDFetcher.formatUUID("74f04a9bb7f9409da940b051f14dd3a5"), punishment.getPunisherUUID());
    }

    @Test
    void setExpiration() {
        Punishment ban = new Punishment(0, Punishment.Type.BAN, "Custom", "N/A", null, UUIDFetcher.formatUUID("74f04a9bb7f9409da940b051f14dd3a5"), null, null, null, Punishment.Status.Active, null, null, new Punishment.MetaData());
        ban.setExpiration(((long) 4.73e+11 + System.currentTimeMillis()));
        assertEquals(((long) 4.73e+11 + System.currentTimeMillis()), ban.getExpiration());
    }

    @Test
    void setMessage() {
        Punishment punishment = new Punishment(0, Punishment.Type.ALL, "Custom", null, ((long) 4.73e+11 + System.currentTimeMillis()), UUIDFetcher.getBLANK_UUID(), "N/A", UUIDFetcher.getBLANK_UUID(), "N/A", Punishment.Status.Active, null, null, new Punishment.MetaData());
        punishment.setMessage("Banned Permanently!");
        assertEquals("Banned Permanently!", punishment.getMessage());
    }

    @Test
    void setStatus() {
        Punishment punishment = new Punishment(0, Punishment.Type.ALL, "Custom", null, ((long) 4.73e+11 + System.currentTimeMillis()), UUIDFetcher.getBLANK_UUID(), "N/A", UUIDFetcher.getBLANK_UUID(), "N/A", Punishment.Status.Pending, null, null, new Punishment.MetaData());
        punishment.setStatus(Punishment.Status.Active);
        assertEquals(Punishment.Status.Active, punishment.getStatus());
    }

    @Test
    void setRemoverUUID() {
        Punishment punishment = new Punishment(0, Punishment.Type.ALL, "Custom", null, ((long) 4.73e+11 + System.currentTimeMillis()), UUIDFetcher.getBLANK_UUID(), "N/A", UUIDFetcher.getBLANK_UUID(), "N/A", Punishment.Status.Pending, null, null, new Punishment.MetaData());
        punishment.setRemoverUUID(UUIDFetcher.formatUUID("74f04a9bb7f9409da940b051f14dd3a5"));
        assertEquals(UUIDFetcher.formatUUID("74f04a9bb7f9409da940b051f14dd3a5"), punishment.getRemoverUUID());
    }

    @Test
    void getIssueDate() {
        Punishment punishment = new Punishment(0, Punishment.Type.ALL, "Custom", null, ((long) 4.73e+11 + System.currentTimeMillis()), UUIDFetcher.getBLANK_UUID(), "N/A", UUIDFetcher.getBLANK_UUID(), "N/A", Punishment.Status.Active, null, null, new Punishment.MetaData());
        assertEquals(punishment.getIssueDate(), "N/A");
    }
}