package com.gustavo.books.catalogue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class BookRepositoryIntegrationTests {

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void clearSeedData() {
        bookRepository.deleteAll();
    }

    @Test
    void searchMatchesTitleAndAuthorIgnoringCase() {
        saveBook("The Silent River", "Octavia Butler", "Science Fiction");
        saveBook("Distant Stars", "URSULA LE GUIN", "Fantasy");
        saveBook("Unrelated Book", "Different Author", "Mystery");

        Page<Book> titleMatches = bookRepository.findBooks(
                "silent RIVER", null, PageRequest.of(0, 10));
        Page<Book> authorMatches = bookRepository.findBooks(
                "ursula le guin", null, PageRequest.of(0, 10));

        assertThat(titleMatches.getContent())
                .extracting(Book::getTitle)
                .containsExactly("The Silent River");
        assertThat(authorMatches.getContent())
                .extracting(Book::getTitle)
                .containsExactly("Distant Stars");
    }

    @Test
    void genreFilterIsCaseInsensitive() {
        saveBook("Fantasy One", "Author One", "Fantasy");
        saveBook("Fantasy Two", "Author Two", "FANTASY");
        saveBook("Mystery One", "Author Three", "Mystery");

        Page<Book> result = bookRepository.findBooks(
                null, "fantasy", PageRequest.of(0, 10, Sort.by("title")));

        assertThat(result.getContent())
                .extracting(Book::getTitle)
                .containsExactly("Fantasy One", "Fantasy Two");
    }

    @Test
    void searchAndGenreMustBothMatch() {
        saveBook("Night Garden", "Author One", "Fantasy");
        saveBook("Night Train", "Author Two", "Mystery");
        saveBook("Morning Garden", "Author Three", "Fantasy");

        Page<Book> result = bookRepository.findBooks(
                "night", "fantasy", PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Book::getTitle)
                .containsExactly("Night Garden");
    }

    @Test
    void paginationReturnsRequestedSliceAndAccurateTotals() {
        saveBook("Alpha", "Author", "Fiction");
        saveBook("Bravo", "Author", "Fiction");
        saveBook("Charlie", "Author", "Fiction");
        saveBook("Delta", "Author", "Fiction");
        saveBook("Echo", "Author", "Fiction");

        Page<Book> result = bookRepository.findBooks(
                null,
                null,
                PageRequest.of(1, 2, Sort.by("title"))
        );

        assertThat(result.getContent())
                .extracting(Book::getTitle)
                .containsExactly("Charlie", "Delta");
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
    }

    @Test
    void unmatchedFiltersReturnAnEmptyPage() {
        saveBook("Known Book", "Known Author", "Fantasy");

        Page<Book> result = bookRepository.findBooks(
                "missing", "fantasy", PageRequest.of(0, 10));

        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    private Book saveBook(String title, String author, String genre) {
        return bookRepository.save(new Book(title, author, genre, 2000));
    }
}
