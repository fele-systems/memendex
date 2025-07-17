package com.systems.fele.memendex_server.meme;

import com.systems.fele.memendex_server.MemendexProperties;
import com.systems.fele.memendex_server.exception.InvalidMemeException;
import com.systems.fele.memendex_server.exception.NoSuchMemeError;
import com.systems.fele.memendex_server.model.Meme;
import com.systems.fele.memendex_server.model.MemeDetailed;
import com.systems.fele.memendex_server.model.MemesType;
import com.systems.fele.memendex_server.model.PaginatedResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartRequest;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
        final var extension = memeService.getMeme(id).orElseThrow(NoSuchMemeError::new).extension();
        final var mediaType = memeService.getThumbnailTo(id, extension, response.getOutputStream());
        response.setContentType(mediaType.toString());
        response.setStatus(200);
    }

    @GetMapping(value = "/{id}/preview")
    public void image(@PathVariable("id") long id, HttpServletResponse response) throws IOException {
        final var meme = memeRepository.findById(id).orElseThrow(NoSuchMemeError::new);

        final var mime = MediaType.parseMediaType(MimeTypeService.extensionToMime(meme.extension()).orElseThrow());
        if (MimeTypeService.isMimeTypeKnown(mime)) {
            final var mediaType = memeService.getImageTo(meme, response.getOutputStream());
            response.setContentType(mediaType.toString());
            response.setStatus(200);
        } else {
            response.getWriter().printf("Cannot preview %s files%n", meme.extension());
            response.setStatus(400);
        }
    }

    @GetMapping(value = "/{id}/download")
    public void download(@PathVariable("id") long id, HttpServletResponse response) throws IOException {
        final var meme = memeRepository.findById(id).orElseThrow(NoSuchMemeError::new);

        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader("Content-Disposition", "attachment; filename=" + meme.fileName());
        memeService.getImageTo(meme, response.getOutputStream());
        response.setStatus(200);
    }

    @PostMapping(value = "upload", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public Meme upload(MultipartHttpServletRequest request, HttpServletResponse response) throws IOException {
        // @RequestPart("meme") MultipartFile meme, @RequestPart("description") String description
        var type = MemesType.valueOf(Objects.requireNonNull(request.getParameter("type"), "The type parameter is missing and it's required"));
        var description = Objects.requireNonNullElse(request.getParameter("description"), "");

        if (type == MemesType.file) {
            var file = request.getFile("file");
            if (file == null) throw new InvalidMemeException("For type `file`, the `file` form-part is required");
            return memeService.saveMeme(description, file);
        } else if (type == MemesType.link) {
            var link = request.getParameter("link");
            if (link == null) throw new InvalidMemeException("For type `link`, the `link` form-part is required");
            return memeService.saveBookmark(description, link);
        } else if (type == MemesType.note) {
            var title = request.getParameter("title");
            if (title == null) throw new InvalidMemeException("For type `note`, the `title` form-part is required");
            return memeService.saveNote(description, title);
        }

        System.out.println(request.getParameter("description"));
        System.out.println(request.getParameter("link"));
        System.out.println(request.getParameter("title"));
        System.out.println(request.getFile("meme"));
        // return memeService.saveMeme(description, meme);
        return null;
    }

    @PatchMapping(value = "edit", consumes = { MediaType.APPLICATION_JSON_VALUE })
    public MemeDetailed edit(@RequestBody MemeDetailed meme){
        memeService.updateMeme(meme);
        return memeService.enrich(memeService.getMeme(meme.id()).orElseThrow(NoSuchMemeError::new));
    }

}
