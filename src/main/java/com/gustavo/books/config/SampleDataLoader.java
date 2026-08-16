package com.gustavo.books.config;

import com.gustavo.books.catalogue.Book;
import com.gustavo.books.catalogue.BookRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SampleDataLoader implements ApplicationRunner {

    private static final int SAMPLE_SIZE = 1_200;

    private static final List<String> AUTHORS = List.of(
            "Octavia Butler",
            "Ursula Le Guin",
            "Kazuo Ishiguro",
            "Toni Morrison",
            "Gabriel Garcia Marquez",
            "Margaret Atwood",
            "George Orwell",
            "Virginia Woolf"
    );

    private static final List<String> GENRES = List.of(
            "Science Fiction",
            "Fantasy",
            "Mystery",
            "Historical Fiction",
            "Literary Fiction",
            "Thriller"
    );

    private static final List<String> TITLE_PREFIXES = List.of(
            "The Last",
            "A Map of",
            "Beyond the",
            "Echoes of",
            "The Hidden",
            "Under the"
    );

    private static final List<String> TITLE_SUBJECTS = List.of(
            "Stars",
            "River",
            "Garden",
            "City",
            "Winter",
            "Ocean",
            "Library",
            "Mountain"
    );

    private final BookRepository bookRepository;

    public SampleDataLoader(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (bookRepository.count() > 0) {
            return;
        }

        List<Book> books = new ArrayList<>(SAMPLE_SIZE);

        for (int index = 0; index < SAMPLE_SIZE; index++) {
            Book book = new Book();
            book.setTitle(createTitle(index));
            book.setAuthor(AUTHORS.get(index % AUTHORS.size()));
            book.setGenre(GENRES.get(index % GENRES.size()));
            book.setPublicationYear(1950 + (index * 7 % 75));
            books.add(book);
        }

        bookRepository.saveAll(books);
    }

    private String createTitle(int index) {
        String prefix = TITLE_PREFIXES.get(index % TITLE_PREFIXES.size());
        String subject = TITLE_SUBJECTS.get((index / TITLE_PREFIXES.size()) % TITLE_SUBJECTS.size());
        return "%s %s %04d".formatted(prefix, subject, index + 1);
    }
}
