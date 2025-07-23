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

    /**
     * Adds or searches for a tag.
     * @param tag The tag. If it has `#` as its first character, it will be removed.
     * @return The tag object
     */
    public Tag addOrFindTag(String tag) {
        tag = tag.trim();
        if (tag.charAt(0) == '#') {
            tag = tag.substring(1);
        }

        var i = tag.indexOf('/');
        if (i >= 0) {
            return tagRepository.addOrFindTag(tag.substring(i), tag.substring(i + 1));
        } else {
            return tagRepository.addOrFindTag(tag, null);
        }
    }
}
