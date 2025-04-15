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
}
