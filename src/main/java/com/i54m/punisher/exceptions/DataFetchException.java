package com.i54m.punisher.exceptions;

public class DataFetchException extends Exception {

    private final String reason, user, requestedInfo, causingClass;
    private final Throwable cause;

    public DataFetchException(String reason, String user, String requestedInfo, String causingClass, Throwable cause){
        super(cause);
        this.reason = reason;
        this.user = user;
        this.requestedInfo = requestedInfo;
        this.causingClass = causingClass;
        this.cause = cause;
    }

    @Override
    public String getMessage() {
        return causingClass + ".class has failed to fetch " + requestedInfo + " on user " + user + ", this was caused by " + cause + ". Reason for data request: " + reason;
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
