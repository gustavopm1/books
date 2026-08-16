package com.gustavo.books.catalogue;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("""
    select b from Book b
    where (
        :search is null
        or lower(b.title) like lower(concat('%', :search, '%'))
        or lower(b.author) like lower(concat('%', :search, '%'))
    )
    and (
        :genre is null
        or lower(b.genre) = lower(:genre)
    )
    """)
    Page<Book> findBooks(
            @Param("search") String search,
            @Param("genre") String genre,
            Pageable pageable
    );
}
