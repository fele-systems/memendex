package com.systems.fele.memendex_server.meme;

import org.h2.api.Trigger;

import java.sql.Connection;
import java.time.ZonedDateTime;

/**
 * Trigger invoked when a meme is updated.
 * <p></p>
 * It's not used when tags are updated, in which case you should
 * use {@link MemeRepository#touch(long)} to manually update the
 * updated field.
 */
public class MemeUpdatedTrigger implements Trigger {
    @Override
    public void fire(Connection connection, Object[] oldRow, Object[] newRow) {
        newRow[newRow.length - 1] = ZonedDateTime.now();
    }
}
