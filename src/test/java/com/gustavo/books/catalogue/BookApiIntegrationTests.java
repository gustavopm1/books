package com.gustavo.books.catalogue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void clearSeedData() {
        bookRepository.deleteAll();
    }

    @Test
    void listingBooksUsesDefaultTitleOrdering() throws Exception {
        saveBook("Charlie", "Author Three", "Mystery");
        saveBook("Alpha", "Author One", "Fantasy");
        saveBook("Bravo", "Author Two", "Science Fiction");

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].title").value("Alpha"))
                .andExpect(jsonPath("$.items[1].title").value("Bravo"))
                .andExpect(jsonPath("$.items[2].title").value("Charlie"));
    }

    @Test
    void listingBooksCombinesSearchAndGenreFilters() throws Exception {
        saveBook("Night Garden", "Author One", "Fantasy");
        saveBook("Night Train", "Author Two", "Mystery");
        saveBook("Morning Garden", "Author Three", "Fantasy");

        mockMvc.perform(get("/api/books")
                        .param("search", " night ")
                        .param("genre", " FANTASY "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Night Garden"))
                .andExpect(jsonPath("$.items[0].genre").value("Fantasy"));
    }

    @Test
    void listingBooksReturnsRequestedPageAndPaginationMetadata() throws Exception {
        saveBook("Alpha", "Author", "Fiction");
        saveBook("Bravo", "Author", "Fiction");
        saveBook("Charlie", "Author", "Fiction");
        saveBook("Delta", "Author", "Fiction");
        saveBook("Echo", "Author", "Fiction");

        mockMvc.perform(get("/api/books")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].title").value("Charlie"))
                .andExpect(jsonPath("$.items[1].title").value("Delta"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalItems").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void validCreationReturnsCreatedBookAndPersistsIt() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Kindred",
                                  "author": "Octavia Butler",
                                  "genre": "Science Fiction",
                                  "publicationYear": 1979
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Kindred"))
                .andExpect(jsonPath("$.author").value("Octavia Butler"))
                .andExpect(jsonPath("$.genre").value("Science Fiction"))
                .andExpect(jsonPath("$.publicationYear").value(1979));

        assertThat(bookRepository.findAll())
                .singleElement()
                .extracting(Book::getTitle)
                .isEqualTo("Kindred");
    }

    @Test
    void blankRequiredFieldReturnsBadRequestWithoutPersisting() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "   ",
                                  "author": "Octavia Butler",
                                  "genre": "Science Fiction",
                                  "publicationYear": 1979
                                }
                                """))
                .andExpect(status().isBadRequest());

        assertThat(bookRepository.count()).isZero();
    }

    @Test
    void deletingExistingBookReturnsNoContentAndRemovesIt() throws Exception {
        Book book = bookRepository.save(
                new Book("Kindred", "Octavia Butler", "Science Fiction", 1979));

        mockMvc.perform(delete("/api/books/{id}", book.getId()))
                .andExpect(status().isNoContent());

        assertThat(bookRepository.existsById(book.getId())).isFalse();
    }

    @Test
    void deletingUnknownBookReturnsNotFoundError() throws Exception {
        long missingId = 999_999L;

        mockMvc.perform(delete("/api/books/{id}", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Book not found: " + missingId));
    }

    private void saveBook(String title, String author, String genre) {
        bookRepository.save(new Book(title, author, genre, 2000));
    }
}
