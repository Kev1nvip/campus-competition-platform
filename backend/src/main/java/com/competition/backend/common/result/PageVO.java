package com.competition.backend.common.result;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@Builder
public class PageVO<T> {

    private List<T> list;
    private Long total;
    private Integer page;
    private Integer size;
    private Integer totalPages;

    // 直接转换，无需类型转换
    public static <T> PageVO<T> of(Page<T> page) {
        return PageVO.<T>builder()
                .list(page.getContent())
                .total(page.getTotalElements())
                .page(page.getNumber() + 1)
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .build();
    }

    // 需要转换VO时使用
    public static <T, R> PageVO<R> of(Page<T> page, Function<T, R> converter) {
        return PageVO.<R>builder()
                .list(page.getContent().stream()
                        .map(converter)
                        .collect(Collectors.toList()))
                .total(page.getTotalElements())
                .page(page.getNumber() + 1)
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .build();
    }
}