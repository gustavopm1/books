package com.gustavo.books.catalogue;

import jakarta.validation.constraints.NotBlank;

public record BookRequest(
        @NotBlank String title,
        @NotBlank String author,
        @NotBlank String genre,
        Integer publicationYear
) {
}