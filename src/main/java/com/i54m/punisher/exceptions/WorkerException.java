package com.i54m.punisher.exceptions;

import com.i54m.punisher.managers.WorkerManager;

public class WorkerException extends Exception {

    private final WorkerManager.Worker worker;
    private final Throwable cause;

    public WorkerException(WorkerManager.Worker worker, Throwable cause) {
        super(cause);
        this.cause = cause;
        this.worker = worker;
    }

    @Override
    public String getMessage() {
        return worker.getName() + " Encountered an error while working: " + cause.toString() + ": " + cause.getMessage();
    }

    @Override
    public void printStackTrace() {
        cause.printStackTrace();
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return cause.getStackTrace();
    }

}
