package com.systems.fele.memendex_server.model;

import java.util.List;

public record PaginatedResponse<T>(
    List<T> data,
    int count,
    int totalCount,
    int pageSize,
    int page,
    boolean hasNext
){
    public static <T> PaginatedResponse<T> empty() {
        return new PaginatedResponse<>(List.of(), 0, 0, 0, 0, false);
    }
}
