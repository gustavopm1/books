package com.gustavo.books.catalogue;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Page<BookResponse> findAll(String search, String genre, Pageable pageable) {
        String normalizedSearch = normalize(search);
        String normalizedGenre = normalize(genre);

        return bookRepository
                .findBooks(normalizedSearch, normalizedGenre, pageable)
                .map(this::toResponse);
    }

    private String normalize(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    @Transactional
    public BookResponse create(BookRequest request) {
        Book book = new Book(
                request.title(),
                request.author(),
                request.genre(),
                request.publicationYear()
        );

        return toResponse(bookRepository.save(book));
    }

    @Transactional
    public void delete(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));

        bookRepository.delete(book);
    }

    private BookResponse toResponse(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getGenre(),
                book.getPublicationYear()
        );
    }
}
