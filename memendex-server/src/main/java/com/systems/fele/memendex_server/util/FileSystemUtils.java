package com.systems.fele.memendex_server.util;

import java.io.File;

public class FileSystemUtils {

    /**
     * Sanitizes the file name so that only the actual file name is returned,
     * stripping any subdirectories specified by the fileName parameter.
     * <p></p>
     * Example:
     * <pre>
     *     sanitizeFileName("/myFile.txt") // returns "myFile.txt"
     * </pre>
     * @param fileName The file name
     * @return Sanitized file name
     */
    public static String sanitizeFileName(String fileName) {
        return new File(fileName).getName();
    }
}
