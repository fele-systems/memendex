package com.systems.fele.memendex_server.meme;

import com.systems.fele.memendex_server.MemendexProperties;
import com.systems.fele.memendex_server.exception.NoSuchMemeError;
import com.systems.fele.memendex_server.model.Meme;
import com.systems.fele.memendex_server.model.MemeDetailed;
import com.systems.fele.memendex_server.model.Tag;
import com.systems.fele.memendex_server.tag.TagRepository;
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
import java.util.Optional;
import java.util.stream.LongStream;

@Service
public class MemeService {
    private final MemendexProperties memendexProperties;
    private final MemeRepository memeRepository;
    private final File cacheDir;
    private final TagRepository tagRepository;
    private final TagToMemeRepository tagToMemeRepository;

    private static final MediaType[] ALLOWED_MIME_TYPES = new MediaType[] {
            MediaType.IMAGE_PNG,
            MediaType.IMAGE_JPEG,
            MediaType.IMAGE_GIF
    };

    public MemeService(MemendexProperties memendexProperties, MemeRepository memeRepository, TagRepository tagRepository, TagToMemeRepository tagToMemeRepository) {
        this.memendexProperties = memendexProperties;
        this.memeRepository = memeRepository;
        this.cacheDir = new File(memendexProperties.cache(), "thumbnails");
        this.tagRepository = tagRepository;
        this.tagToMemeRepository = tagToMemeRepository;

        if (!cacheDir.exists()) cacheDir.mkdirs();

    }

    public MemeDetailed enrich(Meme meme) {
        var tagIds = tagToMemeRepository.getTagsRelatedToMeme(meme.id());
        var tags = LongStream.of(tagIds)
                .mapToObj(tagRepository::getTag)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Tag::toString)
                .toList();
        return new MemeDetailed(meme.id(), meme.fileName(), meme.description(), tags);
    }

    /*private static void readImageFromSteam(InputStream stream) throws IOException {
        var imageInputStream = ImageIO.createImageInputStream(stream);
        var readers = ImageIO.getImageReaders(imageInputStream);

        if (!readers.hasNext()) {
            throw new RuntimeException("Unsupported file type. Could not find appropriate file read for this file format.");
        } else {
            ImageReader reader = readers.next();
            reader.
            ImageReadParam param = reader.getDefaultReadParam();
            reader.setInput(stream, true, true);

            BufferedImage bi;
            try {
                ImageInputStream e = stream;

                try {
                    bi = reader.read(0, param);
                } catch (Throwable var14) {
                    if (stream != null) {
                        try {
                            e.close();
                        } catch (Throwable var13) {
                            var14.addSuppressed(var13);
                        }
                    }

                    throw var14;
                }

                if (stream != null) {
                    stream.close();
                }
            } catch (RuntimeException e) {
                throw new IIOException(e.toString(), e);
            } finally {
                reader.dispose();
            }

            return bi;
        }
    }*/

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


        // var image = ImageIO.read(file.getInputStream());
        // ImageIO.write(image, file.getContentType(), targetFile);
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            IOUtils.copy(file.getInputStream(), fos);
        }

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

    public void updateMeme(MemeDetailed meme) {
        if (meme.fileName() != null || meme.description() != null)
            memeRepository.edit(new Meme(meme.id(), meme.fileName(), meme.description()));
        else
            memeRepository.touch(meme.id());

        if (meme.tags() != null) {
            var currentTagRelation = tagToMemeRepository.getRelationsToMeme(meme.id());

            var newTags = meme.tags().stream()
                    .map(Tag::parse)
                    .map(tag -> tagRepository.addOrFindTag(tag.scope(), tag.value()))
                    .toList();

            var relationsToRemove = currentTagRelation.stream()
                    .filter(r -> newTags.stream().noneMatch(t -> t.id() == r.tagId()))
                    .toList();

            var relationsToCreate = newTags.stream()
                    .filter(t -> currentTagRelation.stream().noneMatch(r -> t.id() == r.tagId()))
                    .toList();

            for (var tag : relationsToCreate) {
                tagToMemeRepository.createRelation(tag.id(), meme.id());
            }

            for (var relation : relationsToRemove) {
                tagToMemeRepository.deleteRelation(relation.id());
                var tagUsage = tagToMemeRepository.countTagReferences(relation.tagId());
                if (tagUsage == 0) tagRepository.deleteTag(relation.tagId());
            }
        }

    }

    public Optional<Meme> getMeme(long id) {
        return memeRepository.findById(id);
    }
}
