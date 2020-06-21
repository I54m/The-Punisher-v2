package com.i54mpenguin.punisher.exceptions;

import com.i54mpenguin.punisher.managers.WorkerManager;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class WorkerException extends Exception {

    private final Exception cause;
    private final WorkerManager.Worker worker;

    @Override
    public String getMessage() {
        return worker.getName() + " Encountered an error while working: " + cause.getMessage();
    }

}
