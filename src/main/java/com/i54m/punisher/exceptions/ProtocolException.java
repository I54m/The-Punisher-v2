package com.i54m.punisher.exceptions;

public class ProtocolException extends Exception {

    private final String where, stream, connection, protocolversion;
    private final Throwable cause;

    public ProtocolException(String where, String stream, String connection, String protocolversion, Throwable cause) {
        super(cause);
        this.where = where;
        this.stream = stream;
        this.connection = connection;
        this.protocolversion = protocolversion;
        this.cause = cause;
    }

    public ProtocolException(String where, Throwable cause) {
        super(cause);
        this.where = where;
        this.cause = cause;
        this.stream = null;
        this.connection = null;
        this.protocolversion = null;
    }

    @Override
    public String getMessage() {
        if (where.equals("Decoder"))
            return "An exception was caught in " + where + ". Stream : " + stream + ". Connection: " + connection + ". ProtocolVersion: " + protocolversion + ". This was caused by " + cause + ".";
        else
            return "An exception was caught in " + where + ". This was caused by " + cause + ".";
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
