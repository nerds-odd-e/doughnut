package com.odde.donut.controllers;

import com.fasterxml.jackson.annotation.JsonView;
import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.AttachBookRequest;
import com.odde.donut.controllers.dto.BookBlockDepthRequest;
import com.odde.donut.controllers.dto.BookLayoutReorganizationSuggestion;
import com.odde.donut.controllers.dto.BookMutationResponse;
import com.odde.donut.controllers.dto.CreateBookBlockFromContentRequest;
import com.odde.donut.entities.Book;
import com.odde.donut.entities.BookBlock;
import com.odde.donut.entities.BookViews;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.book.BookMutationResponseMapper;
import com.odde.donut.services.book.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
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
      @Parameter(description = "Book file") @RequestPart("file") MultipartFile file)
      throws UnexpectedNoAccessRightException, IOException {
    authorizationService.assertAuthorization(notebook);
    if (file == null || file.isEmpty()) {
      throw new ApiException(
          "file is required", ApiError.ErrorType.BINDING_ERROR, "file is required");
    }
    Book body = bookService.attachBook(notebook, metadata, file.getBytes());
    return ResponseEntity.status(HttpStatus.CREATED).body(body);
  }

  @GetMapping("/{notebook}/book")
  @JsonView(BookViews.Full.class)
  public Book getBook(@PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(notebook);
    return bookService.getBookForNotebook(notebook);
  }

  @Operation(
      operationId = "suggestBookLayoutReorganization",
      summary = "Suggest book layout depths via AI (preview only, does not persist)")
  @PostMapping("/{notebook}/book/reorganize-layout/suggest")
  @Transactional(readOnly = true)
  public BookLayoutReorganizationSuggestion suggestBookLayoutReorganization(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    return bookService.suggestLayoutReorganization(notebook);
  }

  @Operation(
      operationId = "applyBookLayoutReorganization",
      summary = "Apply AI-suggested depth changes to the book layout")
  @PostMapping("/{notebook}/book/reorganize-layout/apply")
  @Transactional
  @JsonView(BookViews.Full.class)
  public BookMutationResponse applyBookLayoutReorganization(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @RequestBody @Valid BookLayoutReorganizationSuggestion suggestion)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    Book book = bookService.applyLayoutReorganization(notebook, suggestion);
    return BookMutationResponseMapper.fromBook(book, Set.of());
  }

  @Operation(
      operationId = "createBookBlockFromContent",
      summary = "Create a child book block by splitting at an imported content row")
  @PostMapping("/{notebook}/book/blocks")
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
  public ResponseEntity<Book> createBookBlockFromContent(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @RequestBody @Valid CreateBookBlockFromContentRequest body)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    Book book =
        bookService.createBookBlockFromContent(
            notebook, body.getFromBookContentBlockId(), body.getStructuralTitle());
    return ResponseEntity.status(HttpStatus.CREATED).body(book);
  }

  @Operation(operationId = "changeBookBlockDepth", summary = "Change depth of a book block")
  @PutMapping("/{notebook}/book/blocks/{bookBlock}/depth")
  @Transactional
  @JsonView(BookViews.Full.class)
  public BookMutationResponse changeBookBlockDepth(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("bookBlock") @Schema(type = "integer") BookBlock bookBlock,
      @RequestBody @Valid BookBlockDepthRequest body)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    Book book = bookService.changeBlockDepth(notebook, bookBlock, body.getDirection());
    return BookMutationResponseMapper.fromBook(book, Set.of());
  }

  @Operation(
      operationId = "cancelBookBlock",
      summary = "Cancel a book block (merge content to previous)")
  @DeleteMapping("/{notebook}/book/blocks/{bookBlock}")
  @Transactional
  @JsonView(BookViews.Full.class)
  public BookMutationResponse cancelBookBlock(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("bookBlock") @Schema(type = "integer") BookBlock bookBlock)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    var result = bookService.cancelBlock(notebook, bookBlock);
    return BookMutationResponseMapper.fromBook(result.book(), Set.of(result.predecessorBlockId()));
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
