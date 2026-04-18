package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Book;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.book.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.context.request.WebRequest;

@RestController
@SessionScope
@RequestMapping("/api/books")
class BooksController {

  private final AuthorizationService authorizationService;
  private final BookService bookService;

  BooksController(AuthorizationService authorizationService, BookService bookService) {
    this.authorizationService = authorizationService;
    this.bookService = bookService;
  }

  @Operation(operationId = "getBookFileByBook", summary = "Download book source file")
  @GetMapping(
      value = "/{book}/file",
      produces = {MediaType.APPLICATION_PDF_VALUE, "application/epub+zip"})
  public ResponseEntity<byte[]> getBookFile(
      WebRequest request, @PathVariable("book") @Schema(type = "integer") Book book)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(book.getNotebook());
    var file = bookService.notebookBookFileFromBook(book);
    String etag = file.etag();
    CacheControl cacheControl =
        CacheControl.maxAge(365, TimeUnit.DAYS).cachePrivate().mustRevalidate();
    if (request.checkNotModified(etag)) {
      return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
          .eTag(etag)
          .cacheControl(cacheControl)
          .build();
    }
    return bookService.streamBookFile(file, cacheControl);
  }
}
