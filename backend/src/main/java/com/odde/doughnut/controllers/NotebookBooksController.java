package com.odde.doughnut.controllers;

import com.fasterxml.jackson.annotation.JsonView;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.AttachBookRequest;
import com.odde.doughnut.controllers.dto.BookLastReadPositionRequest;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookRange;
import com.odde.doughnut.entities.BookUserLastReadPosition;
import com.odde.doughnut.entities.BookViews;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.book.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.multipart.MultipartFile;

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

  @Operation(operationId = "attachBook", summary = "Attach book")
  @PostMapping(value = "/{notebook}/attach-book", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
      @Parameter(description = "Attach book metadata as JSON") @RequestPart("metadata") @Valid
          AttachBookRequest metadata,
      @Parameter(description = "PDF file") @RequestPart("file") MultipartFile file)
      throws UnexpectedNoAccessRightException, IOException {
    authorizationService.assertAuthorization(notebook);
    if (file == null || file.isEmpty()) {
      throw new ApiException(
          "file is required", ApiError.ErrorType.BINDING_ERROR, "file is required");
    }
    Book body = bookService.attachBookWithPdf(notebook, metadata, file.getBytes());
    return ResponseEntity.status(HttpStatus.CREATED).body(body);
  }

  @GetMapping("/{notebook}/book")
  @JsonView(BookViews.Full.class)
  public Book getBook(@PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(notebook);
    return bookService.getBookForNotebook(notebook);
  }

  @Operation(operationId = "getNotebookBookReadingPosition", summary = "Get book reading position")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Saved position for the current user",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = BookUserLastReadPosition.class))),
    @ApiResponse(
        responseCode = "204",
        description = "No saved position yet (book exists; user has not stored a snapshot)")
  })
  @GetMapping("/{notebook}/book/reading-position")
  @Transactional(readOnly = true)
  public ResponseEntity<BookUserLastReadPosition> getReadingPosition(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(notebook);
    return bookService
        .getLastReadPosition(notebook, authorizationService.getCurrentUser())
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @Operation(
      operationId = "patchNotebookBookReadingPosition",
      summary = "Save book reading position")
  @PatchMapping("/{notebook}/book/reading-position")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional
  public void patchReadingPosition(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @Valid @RequestBody BookLastReadPositionRequest body)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(notebook);
    bookService.upsertLastReadPosition(notebook, authorizationService.getCurrentUser(), body);
  }

  @Operation(
      operationId = "putNotebookBookRangeReadingRecord",
      summary = "Mark book range as read for the current user")
  @PutMapping("/{notebook}/book/ranges/{bookRange}/reading-record")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional
  public void putRangeReadingRecord(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("bookRange") @Schema(type = "integer") BookRange bookRange)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(notebook);
    bookService.markRangeRead(notebook, authorizationService.getCurrentUser(), bookRange);
  }

  @GetMapping(value = "/{notebook}/book/file", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> getBookFile(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(notebook);
    var pdf = bookService.getBookPdfFile(notebook);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + pdf.attachmentFileName() + "\"")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf.bytes());
  }

  @Operation(operationId = "deleteBook", summary = "Delete book")
  @DeleteMapping("/{notebook}/book")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional
  public void deleteBook(@PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    bookService.deleteBookForNotebook(notebook);
  }
}
