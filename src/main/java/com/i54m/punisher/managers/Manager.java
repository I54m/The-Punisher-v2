package com.i54m.punisher.managers;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.handlers.ErrorHandler;

public interface Manager {

    PunisherPlugin PLUGIN = PunisherPlugin.getInstance();
    ErrorHandler ERROR_HANDLER = ErrorHandler.getINSTANCE();

    void start();
    boolean isStarted();
    void stop();
}
