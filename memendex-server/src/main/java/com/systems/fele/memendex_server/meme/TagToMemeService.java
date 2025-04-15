package com.systems.fele.memendex_server.meme;

import com.systems.fele.memendex_server.model.TagUsage;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagToMemeService {
    private final TagToMemeRepository tagToMemeRepository;

    public TagToMemeService(TagToMemeRepository tagToMemeRepository) {
        this.tagToMemeRepository = tagToMemeRepository;
    }

    public List<TagUsage> getTopTags() {
        return tagToMemeRepository.getTopTags();
    }
}
