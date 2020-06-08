package com.i54mpenguin.punisher.exceptions;

import java.sql.SQLException;

public class PunishmentsDatabaseException extends SQLException {
    private String reason, user, causingClass, command;
    private String[] commandline;
    private Throwable cause;

    public PunishmentsDatabaseException(String reason, String user, String causingClass, Throwable cause) {
        super(cause);
        this.reason = reason;
        this.user = user;
        this.causingClass = causingClass;
        this.cause = cause;
    }

    public PunishmentsDatabaseException(String reason, String user, String causingClass, Throwable cause, String command, String[] commandline) {
        super(cause);
        this.reason = reason;
        this.user = user;
        this.causingClass = causingClass;
        this.cause = cause;
        this.command = command;
        this.commandline = commandline;
    }

    @Override
    public String getMessage() {
        if (user != null) {
            if (command != null && commandline.length > 0) {
                StringBuilder sb = new StringBuilder();
                for (String args : commandline) {
                    sb.append(args).append(" ");
                }
                return causingClass + ".class has failed multiple times to access the mysql database on user " + user + ", this was caused by " + cause
                        + ". Reason for database access request: " + reason + ". Command executed: " + command + " " + sb.toString() + ".";
            }
            return causingClass + ".class has failed multiple times to access the mysql database on user " + user + ", this was caused by " + cause
                    + ". Reason for database access request: " + reason + ".";
        } else {
            if (command != null && commandline.length > 0) {
                StringBuilder sb = new StringBuilder();
                for (String args : commandline) {
                    sb.append(args).append(" ");
                }
                return causingClass + ".class has failed to access the mysql database, this was caused by " + cause
                        + ". Reason for database access request: " + reason + ". Command executed: " + command + " " + sb.toString() + ".";
            }
            return causingClass + ".class has failed to access the mysql database, this was caused by " + cause
                    + ", Cause message: " + cause.getMessage() + ". Reason for database access request: " + reason + ".";
        }
    }
}
