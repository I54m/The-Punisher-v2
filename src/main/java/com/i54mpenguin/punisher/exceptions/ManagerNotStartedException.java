package com.i54mpenguin.punisher.exceptions;

public class ManagerNotStartedException extends Exception {

    private final Class<?> manager;

    public ManagerNotStartedException(Class<?> manager) {
        this.manager = manager;
    }

    @Override
    public String getMessage() {
        return manager.getName() + " has not been started! It must be started using the provided start() method," +
                " this will initialize the manager correctly and ensure that only one instance of it is running at a time.";
    }
}
