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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static dev.jkoe143.libraryapi.constant.Constant.PHOTO_DIRECTORY;

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
        log.info("Uploading photo for book with id: {}", id);
        Book book = getBook(id);
        String photoUrl = photoFunction.apply(id, file);
        book.setPhotoUrl(photoUrl);
        bookRepo.save(book);
        return photoUrl;
    }

    private final Function<String, String> fileExtension = filename -> Optional.of(filename).filter(name -> name.contains("."))
            .map(name -> "." + name.substring(filename.lastIndexOf(".") + 1)).orElse(".png");

    private final BiFunction<String, MultipartFile, String> photoFunction = (id, image) -> {
        String filename = id + fileExtension.apply(image.getOriginalFilename());
        try {
            Path fileStorageLocation = Paths.get(PHOTO_DIRECTORY ).toAbsolutePath().normalize();
            if (!Files.exists(fileStorageLocation)) {
                Files.createDirectories(fileStorageLocation);
            }
            Files.copy(image.getInputStream(), fileStorageLocation.resolve(filename), REPLACE_EXISTING);
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/books/image/" + filename).toUriString();
        } catch (Exception exception) {
            throw new RuntimeException("Unable to save image");
        }
    };

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