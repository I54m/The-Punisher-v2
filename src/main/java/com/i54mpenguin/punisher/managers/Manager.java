package com.i54mpenguin.punisher.managers;

import com.i54mpenguin.punisher.PunisherPlugin;
import com.i54mpenguin.punisher.handlers.ErrorHandler;

public interface Manager {

    PunisherPlugin PLUGIN = PunisherPlugin.getInstance();
    ErrorHandler ERROR_HANDLER = ErrorHandler.getINSTANCE();

    void start();
    boolean isStarted();
    void stop();
}
