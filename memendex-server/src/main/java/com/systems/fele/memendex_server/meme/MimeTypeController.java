package com.systems.fele.memendex_server.meme;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("api/mime")
public class MimeTypeController {
    @GetMapping(value = "known", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MimeTypeService.MimeToExtension> known() {
        return Arrays.stream(MimeTypeService.KNOWN_MIME_TYPES)
                .map(MediaType::toString)
                .map(mime -> new MimeTypeService.MimeToExtension(mime, MimeTypeService.mimeToFileExtension(mime).orElse("data")))
                .toList();
    }
}
