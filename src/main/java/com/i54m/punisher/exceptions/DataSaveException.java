package com.i54m.punisher.exceptions;

public class DataSaveException extends Exception {
    private final String user, data, causingClass;
    private final Throwable cause;

    public DataSaveException(String causingClass , String data, String user, Throwable cause){
        super(cause);
        this.user = user;
        this.data = data;
        this.causingClass = causingClass;
        this.cause = cause;
    }

    @Override
    public String getMessage() {
        return causingClass + ".class has failed to save " + data + " on user " + user + ", this was caused by " + cause + ".";
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
