package dev.jkoe143.libraryapi.service;

import dev.jkoe143.libraryapi.domain.Book;
import dev.jkoe143.libraryapi.repo.BookRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

@Service
@Slf4j
@Transactional(rollbackOn = Exception.class)
@RequiredArgsConstructor
public class BookService {
    private final BookRepo bookRepo;

    public Page<Book> getAllBooks(int page, int size){
        return bookRepo.findAll(PageRequest.of(page, size, Sort.by("title")));
    }

    public Book getBook(String id){
        return bookRepo.findById(id).orElseThrow(() -> new RuntimeException("Book not found"));
    }

    public void deleteBook(String id){
        bookRepo.deleteById(id);
    }

    public String uploadPhoto(String id, MultipartFile file){
        log.info("Saving picture for book ID: {}", id);
        Book book = getBook(id);

        try {
            byte[] imageBytes = file.getBytes();
            String base64Image = "data:" + file.getContentType() + ";base64,"
                    + Base64.getEncoder().encodeToString(imageBytes);

            book.setImageData(base64Image);
            book.setPhotoUrl(base64Image);
            bookRepo.save(book);

            return base64Image;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload photo", e);
        }
    }

    public Book updateBook(Book book) {
        Book existingBook = getBook(book.getId());

        if (book.getTitle() != null) {
            existingBook.setTitle(book.getTitle());
            existingBook.setLibraryUrl(generateLibraryUrl(book.getTitle()));
        }
        if (book.getAuthor() != null) existingBook.setAuthor(book.getAuthor());
        if (book.getPublicationDate() != null) existingBook.setPublicationDate(book.getPublicationDate());
        if (book.getDescription() != null) existingBook.setDescription(book.getDescription());

        return bookRepo.save(existingBook);
    }

    public Book createBook(Book book) {
        if (book.getLibraryUrl() == null || book.getLibraryUrl().isEmpty()) {
            book.setLibraryUrl(generateLibraryUrl(book.getTitle()));
        }
        return bookRepo.save(book);
    }

    private String generateLibraryUrl(String title) {
        String searchTitle = title.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", "")
                .replaceAll("\\s+", "+");
        return "https://www.gutenberg.org/ebooks/search/?query=" + searchTitle;
    }
}