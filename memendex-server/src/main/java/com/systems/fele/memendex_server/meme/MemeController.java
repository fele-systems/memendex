package com.systems.fele.memendex_server.meme;

import com.systems.fele.memendex_server.MemendexProperties;
import com.systems.fele.memendex_server.exception.NoSuchMemeError;
import com.systems.fele.memendex_server.model.Meme;
import com.systems.fele.memendex_server.model.MemeDetailed;
import com.systems.fele.memendex_server.model.PaginatedResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.List;

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

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public MemeDetailed getMeme(@PathVariable("id") long id) {
        return memeService.enrich(memeService.getMeme(id).orElseThrow(NoSuchMemeError::new));
    }

    @GetMapping("list")
    public PaginatedResponse<MemeDetailed> list(@RequestParam(value = "page", required = false, defaultValue = "1") int page, @RequestParam(value = "size", required = false, defaultValue = "100") int size) {
        var memes = memeRepository.listPaginated(page, size);
        if (page == 0) page = 1;
        if (size < 1 || size > 1000) size = 100;
        var totalCount = memeRepository.getTotalCount();
        var hasNext = (page - 1) * size + memes.size() < totalCount;

        return new PaginatedResponse<>(
                memes.stream().map(memeService::enrich).toList(),
                memes.size(),
                totalCount,
                size,
                page,
                hasNext
        );
    }

    @GetMapping("search")
    public PaginatedResponse<MemeDetailed> search(@RequestParam String query, @RequestParam(value = "page", required = false, defaultValue = "1") int page, @RequestParam(value = "size", required = false, defaultValue = "100") int size) {
        if (query.length() < 3)
            return PaginatedResponse.empty();

        if (page == 0) page = 1;
        if (size < 1 || size > 1000) size = 100;

        var memes = memeRepository.powerSearch(query, page, size);
        var totalCount = -1;
        boolean hasNext;

        if (memes.size() > size) {
            hasNext = true;
            memes = memes.subList(0, size);
        } else {
            hasNext = false;
        }

        return new PaginatedResponse<>(
                memes.stream().map(memeService::enrich).toList(),
                memes.size(),
                totalCount,
                size,
                page,
                hasNext
        );
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
    public MemeDetailed edit(@RequestBody MemeDetailed meme){
        memeService.updateMeme(meme);
        return memeService.enrich(memeService.getMeme(meme.id()).orElseThrow(NoSuchMemeError::new));
    }

}
