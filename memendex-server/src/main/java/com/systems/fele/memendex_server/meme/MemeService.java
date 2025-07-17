package com.systems.fele.memendex_server.meme;

import com.systems.fele.memendex_server.MemendexProperties;
import com.systems.fele.memendex_server.exception.NoSuchMemeError;
import com.systems.fele.memendex_server.model.*;
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
import java.util.Optional;
import java.util.stream.LongStream;

@Service
public class MemeService {
    private final MemendexProperties memendexProperties;
    private final MemeRepository memeRepository;
    private final File cacheDir;
    private final TagRepository tagRepository;
    private final TagToMemeRepository tagToMemeRepository;

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
        return new MemeDetailed(meme.id(), meme.type(), meme.fileName(), meme.description(), meme.extension(), tags);
    }

    /**
     * Saves a new meme (type image) in the repository. This function handles the file inside
     * upload location.
     * @param description \The description of the meme to create
     * @param file The @{@link MultipartFile} with the file contents
     * @return The newly created meme
     * @throws IOException If there`s any IO errors
     */
    public Meme saveMeme(String description, MultipartFile file) throws IOException {
        if (file.getContentType() == null || file.getOriginalFilename() == null)
            throw new RuntimeException("Content-Type of file or file name cannot be null");

        final var mimeType = MediaType.parseMediaType(file.getContentType());
        final String fileExtension;
        final boolean processThumbnail;
        if (Arrays.stream(MimeTypeService.KNOWN_MIME_TYPES).noneMatch(mimeType::equalsTypeAndSubtype)) {
            fileExtension = Optional.ofNullable(FileSystemUtils.getExtension(file.getOriginalFilename())).orElse("dat");
            processThumbnail = false;
        } else {
            fileExtension = MimeTypeService.mimeToFileExtension(file.getContentType()).orElse("dat");
            processThumbnail = true;
        }

        var meme = memeRepository.insert(new MemePayload(MemesType.file, file.getOriginalFilename(), description, fileExtension));

        final var fileName = meme.getPhysicalFileName();
        final var targetFile = new File(memendexProperties.uploadLocation(), fileName);

        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            IOUtils.copy(file.getInputStream(), fos);
        }

        if (processThumbnail)
            generateAndSaveThumbnail(meme.id(), fileExtension);

        return meme;
    }

    /**
     * Saves a new meme (type link) in the repository.
     * @param description The description of the meme to create
     * @param link The URL this link will bookmark
     * @return The newly created meme
     */
    public Meme saveBookmark(String description, String link) {
        assert (description != null && link != null);
        return memeRepository.insert(new MemePayload(MemesType.link, link, description, null));
    }

    /**
     * Saves a new meme (type note) in the repository.
     * @param description The description of the meme to create
     * @param title Title for the note
     * @return The newly created meme
     */
    public Meme saveNote(String description, String title) {
        assert (description != null && title != null);
        return memeRepository.insert(new MemePayload(MemesType.note, title, description, null));
    }

    /**
     * Returns the file which is the thumbnail of parameter fileName
     * @param id meme id
     * @return The thumbnail {@link File} object
     */
    public File getThumbnailFileName(long id) {
        return new File(cacheDir, id +  ".jpeg");
    }

    /**
     * Generates a thumbnail and write it to the file system
     * @param id meme id
     * @param extension original file name extension
     * @return The thumbnail {@link File} object
     * @throws IOException If any error
     */
    private File generateAndSaveThumbnail(long id, String extension) throws IOException {
        final var thumbnailFile = getThumbnailFileName(id);
        var thumbImg = generateThumbnail(id + "." + extension);
        ImageIO.write(thumbImg, "jpeg", thumbnailFile);
        return thumbnailFile;
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

    public MediaType getThumbnailTo(long id, String extension, OutputStream outputStream) throws IOException {
        final var meme = memeRepository.findById(id).orElseThrow(NoSuchMemeError::new);
        final var thumbnailFile = getThumbnailFileName(id);
        if (!thumbnailFile.exists()) {
            generateAndSaveThumbnail(id, extension);
        }

        try(FileInputStream input = new FileInputStream(thumbnailFile)) {
            IOUtils.copy(input, outputStream);
        }

        // Thumbnails are always JPEG
        return MediaType.IMAGE_JPEG;
    }

    public MediaType getImageTo(Meme meme, ServletOutputStream outputStream) throws IOException {
        try(FileInputStream input = new FileInputStream(new File(memendexProperties.uploadLocation(), meme.getPhysicalFileName()))) {
            IOUtils.copy(input, outputStream);
        }

        return new MediaType("image", meme.extension());
    }

    public void updateMeme(MemeDetailed meme) {
        if (meme.fileName() != null || meme.description() != null)
            memeRepository.update(meme.id(), new MemePayload(meme.type(), meme.fileName(), meme.description(), null));
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
