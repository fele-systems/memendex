package com.systems.fele.memendex_server.meme;

import com.systems.fele.memendex_server.MemendexProperties;
import com.systems.fele.memendex_server.exception.NoSuchMemeError;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.*;
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

    @GetMapping(value = "/{id}/thumbnail")
    public void thumbnail(@PathVariable("id") long id, HttpServletResponse response) throws IOException {
        final var mediaType = memeService.getThumbnailTo(id, response.getOutputStream());
        response.setContentType(mediaType.toString());
        response.setStatus(200);
    }

    @GetMapping(value = "/{id}/image")
    public void image(@PathVariable("id") long id, HttpServletResponse response) throws IOException {
        final var mediaType = memeService.getImageTo(id, response.getOutputStream());
        response.setContentType(mediaType.toString());
        response.setStatus(200);
    }

    @PostMapping(value = "upload", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public Meme upload(@RequestPart("meme") MultipartFile meme, @RequestPart("description") String description, HttpServletResponse response) throws IOException {
        return memeService.saveMeme(description, meme);
    }

    @PatchMapping(value = "edit", consumes = { MediaType.APPLICATION_JSON_VALUE })
    public Meme edit(@RequestBody Meme meme){
        memeRepository.edit(meme);
        return memeRepository.findById(meme.id()).orElseThrow(NoSuchMemeError::new);
    }

}
