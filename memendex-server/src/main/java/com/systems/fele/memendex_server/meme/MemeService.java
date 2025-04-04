package com.systems.fele.memendex_server.meme;

import com.systems.fele.memendex_server.MemendexProperties;
import com.systems.fele.memendex_server.exception.NoSuchMemeError;
import com.systems.fele.memendex_server.util.FileSystemUtils;
import jakarta.servlet.ServletOutputStream;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.util.Arrays;
import java.util.Objects;

@Service
public class MemeService {
    private final MemendexProperties memendexProperties;
    private final MemeRepository memeRepository;
    private final File cacheDir;
    
    private static final MediaType[] ALLOWED_MIME_TYPES = new MediaType[] {
            MediaType.IMAGE_PNG,
            MediaType.IMAGE_JPEG,
            MediaType.IMAGE_GIF
    };
    public MemeService(MemendexProperties memendexProperties, MemeRepository memeRepository) {
        this.memendexProperties = memendexProperties;
        this.memeRepository = memeRepository;
        this.cacheDir = new File(memendexProperties.cache(), "thumbnails");

        if (!cacheDir.exists()) cacheDir.mkdirs();

    }

    /**
     * Saves a new meme in the repository. This function handles the file inside
     * upload location.
     * @param description The description of the meme to create
     * @param file The @{@link MultipartFile} with the file contents
     * @return The newly created meme
     * @throws IOException If there`s any IO errors
     */
    public Meme saveMeme(String description, MultipartFile file) throws IOException {
        if (Arrays.stream(ALLOWED_MIME_TYPES).filter(mediaType -> mediaType.toString().equals(file.getContentType())).findAny().isEmpty()) {
            throw new RuntimeException("Unsupported media type: " + file.getContentType());
        }

        final var fileName = FileSystemUtils.sanitizeFileName(Objects.requireNonNull(file.getOriginalFilename()));
        final var targetFile = new File(memendexProperties.uploadLocation(), fileName);

        var image = ImageIO.read(file.getInputStream());

        ImageIO.write(image, file.getContentType(), targetFile);

        generateAndSaveThumbnail(fileName);

        return memeRepository.insert(new Meme(0, fileName, description));
    }

    public BufferedImage getImage(Meme meme) throws IOException {
        return getImage(meme.fileName());
    }

    private BufferedImage getImage(String fileName) throws IOException {
        return ImageIO.read(new File(memendexProperties.uploadLocation(), fileName));
    }

    /**
     * Returns the file which is the thumbnail of parameter fileName
     * @param fileName The original file
     * @return The thumbnail {@link File} object
     */
    public File getAssociatedThumbnailFile(String fileName) {
        final var sanitizedFileName = FileSystemUtils.sanitizeFileName(fileName);
        final var i = sanitizedFileName.lastIndexOf('.');
        final var fileNameNoExtension = sanitizedFileName.substring(0, i);
        return new File(cacheDir, fileNameNoExtension +  ".jpeg");
    }

    /**
     * Generates a thumbnail and write it to the file system
     * @param fileName The original file
     * @return The thumbnail {@link File} object
     * @throws IOException If any error
     */
    private File generateAndSaveThumbnail(String fileName) throws IOException {
        final var thumbnailFile = getAssociatedThumbnailFile(fileName);
        var thumbImg = generateThumbnail(fileName);
        ImageIO.write(thumbImg, "jpeg", thumbnailFile);
        return thumbnailFile;
    }

    public BufferedImage generateThumbnail(Meme meme) throws IOException {
        return generateThumbnail(meme.fileName());
    }

    public BufferedImage generateThumbnail(String fileName) throws IOException {
        var img = ImageIO.read(new File(memendexProperties.uploadLocation(), fileName));
        return generateThumbnail(img);
    }

    public BufferedImage generateThumbnail(RenderedImage img) throws IOException {
        double ratio = (double) img.getHeight() / (double) img.getWidth();
        int desiredHeight = 100;
        int desiredWidth = (int) (desiredHeight / ratio);
        BufferedImage bi = getCompatibleImage(desiredWidth, desiredHeight);
        // var bi = img;
        Graphics2D g2d = bi.createGraphics();

        double xScale = (double) desiredWidth / img.getWidth();
        double yScale = (double) desiredHeight / img.getHeight();

        AffineTransform at = AffineTransform.getScaleInstance(xScale,yScale);
        g2d.drawRenderedImage(img, at);
        g2d.dispose();
        return bi;
    }

    private BufferedImage getCompatibleImage(int w, int h) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        return gc.createCompatibleImage(w, h);
    }

    public MediaType getThumbnailTo(long id, OutputStream outputStream) throws IOException {
        final var meme = memeRepository.findById(id).orElseThrow(NoSuchMemeError::new);
        final var thumbnailFile = getAssociatedThumbnailFile(meme.fileName());
        if (!thumbnailFile.exists()) {
            generateAndSaveThumbnail(meme.fileName());
        }

        try(FileInputStream input = new FileInputStream(thumbnailFile)) {
            IOUtils.copy(input, outputStream);
        }

        // Thumbnails are always JPEG
        return MediaType.IMAGE_JPEG;
    }

    public MediaType getImageTo(long id, ServletOutputStream outputStream) throws IOException {
        final var meme = memeRepository.findById(id).orElseThrow(NoSuchMemeError::new);
        final var i = meme.fileName().lastIndexOf('.');
        final var extension = meme.fileName().substring(i + 1);

        try(FileInputStream input = new FileInputStream(new File(memendexProperties.uploadLocation(), meme.fileName()))) {
            IOUtils.copy(input, outputStream);
        }

        return new MediaType("image", extension);
    }
}
