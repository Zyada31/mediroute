package com.mediroute.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Standard paginated response envelope")
public class PageResponse<T> {
    @Schema(description = "Page items")
    private List<T> items;
    @Schema(description = "Zero-based page index")
    private int page;
    @Schema(description = "Page size")
    private int size;
    @Schema(description = "Total number of elements across all pages")
    private long totalElements;
    @Schema(description = "Total number of pages")
    private int totalPages;
    @Schema(description = "Sort directive, e.g. 'id,asc'")
    private String sort;

    public PageResponse() {}

    public PageResponse(List<T> items, int page, int size, long totalElements, int totalPages, String sort) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.sort = sort;
    }

    public List<T> getItems() { return items; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public String getSort() { return sort; }

    public void setItems(List<T> items) { this.items = items; }
    public void setPage(int page) { this.page = page; }
    public void setSize(int size) { this.size = size; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public void setSort(String sort) { this.sort = sort; }
}


