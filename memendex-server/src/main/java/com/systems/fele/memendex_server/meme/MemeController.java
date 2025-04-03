package com.systems.fele.memendex_server.meme;

import com.systems.fele.memendex_server.MemendexProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/memes")
public class MemeController {
    private final MemeService memeService;
    private final MemendexProperties memendexProperties;
    private final MemeRepository memeRepository;

    @Autowired
    public MemeController(MemeService memeService, MemendexProperties memendexProperties, MemeRepository memeRepository) {
        this.memeService = memeService;
        this.memendexProperties = memendexProperties;
        this.memeRepository = memeRepository;
    }

    @GetMapping("list")
    public List<Meme> list() {
        return memeRepository.list();
    }

    @GetMapping("search")
    public List<Meme> search(@RequestParam String query) {
        if (query.length() < 3)
            return List.of();

        return memeRepository.powerSearch(query);
    }

    // TODO Send correct media type
    @GetMapping(value = "/{id}/thumbnail", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] thumbnail(@PathVariable("id") long id) throws IOException {
        var thumb = memeService.generateThumbnail(memeRepository.list().stream().filter(meme -> meme.id() == id).findFirst().orElseThrow());
        var bos = new ByteArrayOutputStream();
        ImageIO.write(thumb, "png", bos);
        return bos.toByteArray();
    }

    // TODO Send correct media type
    @GetMapping(value = "/{id}/image", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] image(@PathVariable("id") long id) throws IOException {
        var thumb = memeService.getImage(memeRepository.list().stream().filter(meme -> meme.id() == id).findFirst().orElseThrow());
        var bos = new ByteArrayOutputStream();
        ImageIO.write(thumb, "png", bos);
        System.out.println(memeRepository.list().stream().filter(meme -> meme.id() == id).findFirst().orElseThrow().description());
        return bos.toByteArray();
    }

    @PostMapping(value = "upload", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public Meme upload(@RequestPart("meme") MultipartFile meme, @RequestPart("description") String description, HttpServletResponse response) throws IOException {
        final var fileName = Objects.requireNonNull(meme.getOriginalFilename());
        final var targetFile = new File(memendexProperties.uploadLocation(), fileName);

        try (FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            var inputStream = meme.getInputStream();
            var buffer = new byte[256];
            int length = 0;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        } catch (FileNotFoundException e) {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Could not create file in " + memendexProperties.uploadLocation());
        }

        return memeRepository.insert(new Meme(0, fileName, description));
    }
}
