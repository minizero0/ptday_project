package com.gym.common.response;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 페이징 목록 응답 (CLAUDE.md §5). Spring Page 를 안정적인 형태로 감싼다.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
