package com.odde.doughnut.controllers;

import com.fasterxml.jackson.annotation.JsonView;
import com.odde.doughnut.controllers.dto.AttachBookRequest;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookViews;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.book.BookService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/notebooks")
class NotebookBooksController {

  private final AuthorizationService authorizationService;
  private final BookService bookService;

  NotebookBooksController(AuthorizationService authorizationService, BookService bookService) {
    this.authorizationService = authorizationService;
    this.bookService = bookService;
  }

  @PostMapping(value = "/{notebook}/attach-book", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Transactional
  @JsonView(BookViews.Full.class)
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Created",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Book.class)))
  })
  public ResponseEntity<Book> attachBook(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @Valid @RequestBody AttachBookRequest request)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    Book body = bookService.attachBook(notebook, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(body);
  }

  @GetMapping("/{notebook}/books/{book}")
  @JsonView(BookViews.Full.class)
  public Book getBook(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("book") @Schema(type = "integer") Book book)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(notebook);
    return bookService.requireBookInNotebook(notebook, book);
  }
}
