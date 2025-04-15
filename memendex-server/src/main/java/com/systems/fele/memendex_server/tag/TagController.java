package com.systems.fele.memendex_server.tag;

import com.systems.fele.memendex_server.meme.TagToMemeRepository;
import com.systems.fele.memendex_server.model.Tag;
import com.systems.fele.memendex_server.model.TagUsage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {
    private final TagService tagService;
    private final TagToMemeRepository tagToMemeRepository;

    public TagController(TagService tagService, TagToMemeRepository tagToMemeRepository) {
        this.tagService = tagService;
        this.tagToMemeRepository = tagToMemeRepository;
    }

    @GetMapping("search")
    public List<Tag> search(@RequestParam("q") String searchTerm) {
        return tagService.searchTags(searchTerm);
    }

    @GetMapping("suggestions")
    public List<TagUsage> suggestions(@RequestParam(value = "q", required = false) String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank())
            return tagToMemeRepository.getTopTags();
        else
            return tagToMemeRepository.getSuggestions(searchTerm);
    }
}
