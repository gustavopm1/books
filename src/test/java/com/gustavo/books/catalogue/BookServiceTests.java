package com.gustavo.books.catalogue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTests {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    void findAllNormalizesFiltersAndMapsBooksToResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Book book = book(42L, "Night Garden", "Author One", "Fantasy", 2001);
        when(bookRepository.findBooks("night", "FANTASY", pageable))
                .thenReturn(new PageImpl<>(List.of(book), pageable, 1));

        Page<BookResponse> result = bookService.findAll(
                "  night  ", " FANTASY ", pageable);

        assertThat(result.getContent())
                .containsExactly(new BookResponse(
                        42L, "Night Garden", "Author One", "Fantasy", 2001));
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(bookRepository).findBooks("night", "FANTASY", pageable);
    }

    @Test
    void findAllTreatsBlankFiltersAsAbsent() {
        Pageable pageable = PageRequest.of(0, 20);
        when(bookRepository.findBooks(null, null, pageable)).thenReturn(Page.empty(pageable));

        Page<BookResponse> result = bookService.findAll("   ", "", pageable);

        assertThat(result).isEmpty();
        verify(bookRepository).findBooks(null, null, pageable);
    }

    @Test
    void createMapsRequestToEntityAndReturnsPersistedResponse() {
        BookRequest request = new BookRequest(
                "Kindred", "Octavia Butler", "Science Fiction", 1979);
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book savedBook = invocation.getArgument(0);
            savedBook.setId(7L);
            return savedBook;
        });

        BookResponse response = bookService.create(request);

        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(bookCaptor.capture());
        assertThat(bookCaptor.getValue())
                .extracting(
                        Book::getTitle,
                        Book::getAuthor,
                        Book::getGenre,
                        Book::getPublicationYear
                )
                .containsExactly("Kindred", "Octavia Butler", "Science Fiction", 1979);
        assertThat(response).isEqualTo(new BookResponse(
                7L, "Kindred", "Octavia Butler", "Science Fiction", 1979));
    }

    @Test
    void deleteRemovesExistingBook() {
        Book existingBook = book(9L, "Kindred", "Octavia Butler", "Science Fiction", 1979);
        when(bookRepository.findById(9L)).thenReturn(Optional.of(existingBook));

        bookService.delete(9L);

        verify(bookRepository).delete(existingBook);
    }

    @Test
    void deleteThrowsMeaningfulExceptionWhenBookDoesNotExist() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.delete(99L))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessage("Book not found: 99");
        verify(bookRepository, never()).delete(any(Book.class));
    }

    private Book book(
            Long id,
            String title,
            String author,
            String genre,
            Integer publicationYear
    ) {
        Book book = new Book(title, author, genre, publicationYear);
        book.setId(id);
        return book;
    }
}
