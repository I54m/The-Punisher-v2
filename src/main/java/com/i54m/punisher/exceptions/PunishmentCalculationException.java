package com.i54m.punisher.exceptions;

public class PunishmentCalculationException extends Exception {

    private final String reasonForFailure, stage;
    private Throwable cause;

    public PunishmentCalculationException(String reasonForFailure, String stage, Throwable cause){
        super(cause);
        this.reasonForFailure = reasonForFailure;
        this.stage = stage;
        this.cause = cause;
    }

    public PunishmentCalculationException(String reasonForFailure, String stage){
        this.reasonForFailure = reasonForFailure;
        this.stage = stage;
    }

    @Override
    public String getMessage() {
            return cause != null ? "Unable to calculate " + stage + " for automatic punishment because: " + reasonForFailure + ". This Error was caused by " + cause + ", Cause message: " + cause.getMessage() :
                    "Unable to calculate " + stage + " for automatic punishment because: " + reasonForFailure + ". Cause was unknown.";
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
