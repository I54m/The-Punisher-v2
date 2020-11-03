package com.i54m.punisher.exceptions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataFetchException extends Exception {

    private final String reason, user, requestedInfo, causingClass;
    private final Throwable cause;

    @Deprecated
    public DataFetchException(String reason, String user, String requestedInfo, String causingClass, Throwable cause){// TODO: 3/11/2020 shuffle args to make more sense
        super(cause);
        this.reason = reason;
        this.user = user;
        this.requestedInfo = requestedInfo;
        this.causingClass = causingClass;
        this.cause = cause;
    }

    public DataFetchException(@NotNull String causingClass, @NotNull String requestedInfo, @NotNull String user, @Nullable Throwable cause, @NotNull String reason){
        super(cause);
        this.causingClass = causingClass;
        this.requestedInfo = requestedInfo;
        this.user = user;
        this.cause = cause;
        this.reason = reason;
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
