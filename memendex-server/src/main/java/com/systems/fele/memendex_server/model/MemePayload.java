package com.systems.fele.memendex_server.model;

public record MemePayload(MemesType type, String fileName, String description, String extension) {
}
