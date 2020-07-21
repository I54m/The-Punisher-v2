package com.i54m.punisher.managers.storage;

import lombok.Getter;

public enum StorageType {

    FLAT_FILE(FlatFileManager.getINSTANCE()),
    SQLITE(SQLManager.getINSTANCE()),
    MY_SQL(SQLManager.getINSTANCE());

    @Getter
    private final StorageManager storageManager;

    StorageType (StorageManager storageManager) {
        this.storageManager = storageManager;
    }
}
