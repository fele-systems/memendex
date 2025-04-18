package com.systems.fele.memendex_server.model;

import java.util.List;

public record MemeDetailed(long id,
                           String fileName,
                           String description,
                           String extension,
                           List<String> tags) {
}
