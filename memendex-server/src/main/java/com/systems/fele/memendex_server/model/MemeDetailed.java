package com.systems.fele.memendex_server.model;

import java.util.List;

public record MemeDetailed(long id,
                           MemesType type,
                           String fileName,
                           String description,
                           String extension,
                           List<String> tags) {
}
