package com.gustavo.books.catalogue;

public record BookResponse(
        Long id,
        String title,
        String author,
        String genre,
        Integer publicationYear
) {
}