package com.java_web.backend.Common.DTO;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public class PagedResponse<T> {
    private List<T> items;
    private long total;
    private long page;
    private long pageSize;
    private long totalPages;

    public PagedResponse() {
    }

    public PagedResponse(List<T> items, long total, long page, long pageSize, long totalPages) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
    }

    public static <T> PagedResponse<T> of(IPage<T> pageData) {
        long totalPages = Math.max(pageData.getPages(), 1L);
        return new PagedResponse<>(
                pageData.getRecords(),
                pageData.getTotal(),
                pageData.getCurrent(),
                pageData.getSize(),
                totalPages
        );
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPage() {
        return page;
    }

    public void setPage(long page) {
        this.page = page;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(long totalPages) {
        this.totalPages = totalPages;
    }
}
