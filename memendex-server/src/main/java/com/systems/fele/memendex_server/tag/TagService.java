package com.systems.fele.memendex_server.tag;

import com.systems.fele.memendex_server.model.Tag;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TagService {
    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public List<Tag> searchTags(String searchTerm) {
        return tagRepository.search(searchTerm);
    }

    public Optional<Tag> findById(long id) {
        return tagRepository.getTag(id);
    }
}
