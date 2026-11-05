package com.cuymonitor.backend.domain.model;

// One page of a long list: page counts from 0, and nobody can ask for more than MAX_SIZE rows at once.
public record Paging(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public Paging {
        if (page < 0) {
            throw new IllegalArgumentException("page can't be negative");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        }
    }

    // fills what was not given: the first page, DEFAULT_SIZE rows
    public static Paging of(Integer page, Integer size) {
        return new Paging(page == null ? 0 : page, size == null ? DEFAULT_SIZE : size);
    }

    public long offset() {
        return (long) page * size;
    }
}
