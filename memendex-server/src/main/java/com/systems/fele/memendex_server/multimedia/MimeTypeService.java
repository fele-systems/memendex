package com.systems.fele.memendex_server.multimedia;

import org.springframework.http.MediaType;

import javax.print.attribute.standard.Media;
import java.util.Arrays;
import java.util.Optional;

public class MimeTypeService {

    public static final MediaType VIDEO_MP4 = new MediaType("video", "mp4");
    public static final MediaType IMAGE_HEIF = new MediaType("image", "heif");

    public static final MediaType[] KNOWN_MIME_TYPES = new MediaType[] {
            MediaType.IMAGE_PNG,
            MediaType.IMAGE_JPEG,
            MediaType.IMAGE_GIF,
            IMAGE_HEIF,
            VIDEO_MP4
    };

    public record MimeToExtension(String mimeType, String extension) {
        public boolean hasMimeType(String mimeType) {
            return this.mimeType.equals(mimeType);
        }

        public boolean hasExtension(String extension) {
            return this.extension.equals(extension);
        }
    }

    private static final MimeToExtension[] MIME_TO_EXTENSION_TABLE = new MimeToExtension[]{
            new MimeToExtension(MediaType.IMAGE_PNG_VALUE, "png"),
            new MimeToExtension(MediaType.IMAGE_JPEG_VALUE, "jpeg"),
            new MimeToExtension(MediaType.IMAGE_GIF_VALUE, "gif"),
            new MimeToExtension(IMAGE_HEIF.toString(), "heic"),
            new MimeToExtension(VIDEO_MP4.toString(), "mp4"),
    };

    /**
     * Returns an extension based on mime type. The extension does not contain a "."
     * @param mimeType mime type
     * @return extension
     */
    public static Optional<String> mimeToFileExtension(String mimeType) {
        return Arrays.stream(MIME_TO_EXTENSION_TABLE)
                .filter(to -> to.hasMimeType(mimeType))
                .findFirst()
                .map(MimeToExtension::extension);
    }

    /**
     * Returns a mime type based on the extension value. The extension does not contain a "."
     * @param extension extension
     * @return mime type
     */
    public static Optional<String> extensionToMime(String extension) {
        return Arrays.stream(MIME_TO_EXTENSION_TABLE)
                .filter(to -> to.hasExtension(extension))
                .findFirst()
                .map(MimeToExtension::mimeType);
    }

    /**
     * Tests if the given media type is known for generating thumbnails and previews
     * @param mediaType media type
     * @return true if known
     */
    public static boolean isMimeTypeKnown(MediaType mediaType) {
        return Arrays.stream(KNOWN_MIME_TYPES).anyMatch(mediaType::equalsTypeAndSubtype);
    }
}
