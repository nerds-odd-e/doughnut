package com.odde.doughnut.controllers;

import com.fasterxml.jackson.annotation.JsonView;
import com.odde.doughnut.controllers.dto.AttachBookRequest;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookViews;
import com.odde.doughnut.entities.Notebook;
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
import org.springframework.web.server.ResponseStatusException;

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
  public ResponseEntity<Book> attachBookMultipart(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @Parameter(description = "Attach book metadata as JSON") @RequestPart("metadata") @Valid
          AttachBookRequest metadata,
      @Parameter(description = "PDF file") @RequestPart("file") MultipartFile file)
      throws UnexpectedNoAccessRightException, IOException {
    authorizationService.assertAuthorization(notebook);
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
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
}
