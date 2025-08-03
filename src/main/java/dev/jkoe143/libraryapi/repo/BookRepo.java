package dev.jkoe143.libraryapi.repo;

import dev.jkoe143.libraryapi.domain.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;


@Repository
public interface BookRepo extends JpaRepository<Book, String> {
    Optional<Book> findById(String id);
}
