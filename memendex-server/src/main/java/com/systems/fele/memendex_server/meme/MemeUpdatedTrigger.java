package com.systems.fele.memendex_server.meme;

import org.h2.api.Trigger;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZonedDateTime;

public class MemeUpdatedTrigger implements Trigger {

    @Override
    public void fire(Connection connection, Object[] oldRow, Object[] newRow) throws SQLException {
        System.out.printf("Previous value: %s%n", oldRow[oldRow.length-1]);
        System.out.printf("New value: %s%n", newRow[newRow.length-1]);
        newRow[newRow.length - 1] = ZonedDateTime.now();
    }
}
