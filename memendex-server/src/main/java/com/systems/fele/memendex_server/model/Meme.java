package com.systems.fele.memendex_server.model;

import java.time.ZonedDateTime;

/**
 * Meme record
 * @param id Unique id inside database. Used to determine the physical file name
 * @param fileName Virtual file name. Displayed only for the user. May have duplicates
 * @param description Description of the file.
 * @param extension Extension of the file
 * @param create Created timestamp
 * @param updated Last updated timestamp
 */
public record Meme(
        long id,
        String fileName,
        String description,
        String extension,
        ZonedDateTime create,
        ZonedDateTime updated) {

    /**
     * Returns the actual file name inside the disk
     * @return File name with extension
     */
    public String getPhysicalFileName() {
        return "%d.%s".formatted(id, extension);
    }

}
