package com.odde.donut.services.book;

import static com.odde.donut.services.book.BookReadingWireConstants.BOOK_FORMAT_EPUB;
import static com.odde.donut.services.book.BookReadingWireConstants.BOOK_FORMAT_PDF;

import com.odde.donut.entities.Book;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.server.ResponseStatusException;

public record NotebookBookFile(byte[] bytes, String baseName, String etag, BookFormat format) {

  /** The Book's file with {@code etagSource} identifying these exact bytes. */
  static NotebookBookFile of(Book book, byte[] fileBytes, byte[] etagSource) {
    String format = book.getFormat();
    if (!BOOK_FORMAT_PDF.equals(format) && !BOOK_FORMAT_EPUB.equals(format)) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "unsupported book format: " + format);
    }
    return new NotebookBookFile(
        fileBytes,
        sanitizeFileName(book.getBookName()),
        etagFor(etagSource),
        BookFormat.fromString(format));
  }

  ResponseEntity<byte[]> stream(CacheControl cacheControl) {
    ResponseEntity<Resource> streamed = format.streamFile(bytes, baseName, etag, cacheControl);
    ByteArrayResource body = (ByteArrayResource) streamed.getBody();
    return ResponseEntity.status(streamed.getStatusCode())
        .headers(streamed.getHeaders())
        .body(body.getByteArray());
  }

  private static String etagFor(byte[] etagSource) {
    return "\"" + DigestUtils.md5DigestAsHex(etagSource) + "\"";
  }

  private static String sanitizeFileName(String fileName) {
    return fileName.replaceAll("[\\/:*?\"<>|]", "_");
  }
}
