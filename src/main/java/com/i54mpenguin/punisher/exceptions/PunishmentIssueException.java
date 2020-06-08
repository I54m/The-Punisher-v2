package com.i54mpenguin.punisher.exceptions;

import com.i54mpenguin.punisher.objects.Punishment;

public class PunishmentIssueException extends Exception {

    private String reasonForFailure;
    private Punishment punishment;
    private Throwable cause;

    public PunishmentIssueException(String reasonForFailure, Punishment punishment, Throwable cause){
        super(cause);
        this.reasonForFailure = reasonForFailure;
        this.punishment = punishment;
        this.cause = cause;
    }

    public PunishmentIssueException(String reasonForFailure, Punishment punishment){
        this.reasonForFailure = reasonForFailure;
        this.punishment = punishment;
    }

    @Override
    public String getMessage() {
        return cause != null ?
                "Punishment: " + punishment.toString() + " Was unable to be Issued because: " + reasonForFailure + ". This Error was caused by " + cause
                : "Punishment: " + punishment.toString() + " Was unable to be Issued because: " + reasonForFailure + ". Cause was unknown.";
    }
}
