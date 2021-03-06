package com.i54m.punisher.exceptions;

import com.i54m.punisher.objects.Punishment;

public class PunishmentIssueException extends Exception {

    private final String reasonForFailure;
    private final Punishment punishment;
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

    @Override
    public void printStackTrace() {
        if (cause != null)
            cause.printStackTrace();
        else
            super.printStackTrace();
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        if (cause != null)
            return cause.getStackTrace();
        else
            return super.getStackTrace();
    }
}
