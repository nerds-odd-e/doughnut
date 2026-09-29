package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.BookBlockReadingRecordListItem;
import com.odde.donut.controllers.dto.BookBlockReadingRecordPutRequest;
import com.odde.donut.controllers.dto.BookLastReadPositionRequest;
import com.odde.donut.controllers.dto.BookUserLastReadPositionResponse;
import com.odde.donut.entities.BookBlock;
import com.odde.donut.entities.BookBlockReadingRecord;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.book.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** The current user's reading progress in a notebook's book: position and block reading marks. */
@RestController
@RequestMapping("/api/notebooks")
@Tag(name = "notebook-books-controller")
class NotebookBookReadingController {

  private final AuthorizationService authorizationService;
  private final BookService bookService;

  NotebookBookReadingController(
      AuthorizationService authorizationService, BookService bookService) {
    this.authorizationService = authorizationService;
    this.bookService = bookService;
  }

  @Operation(
      operationId = "getNotebookBookReadingRecords",
      summary = "List reading records for the notebook book (current user)")
  @GetMapping("/{notebook}/book/reading-records")
  @Transactional(readOnly = true)
  public List<BookBlockReadingRecordListItem> getBookReadingRecords(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(notebook);
    return bookService.listReadingRecordsForBook(notebook, authorizationService.getCurrentUser());
  }

  @Operation(operationId = "getNotebookBookReadingPosition", summary = "Get book reading position")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Saved position for the current user",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = BookUserLastReadPositionResponse.class))),
    @ApiResponse(
        responseCode = "204",
        description = "No saved position yet (book exists; user has not stored a snapshot)")
  })
  @GetMapping("/{notebook}/book/reading-position")
  @Transactional(readOnly = true)
  public ResponseEntity<BookUserLastReadPositionResponse> getReadingPosition(
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
      operationId = "putNotebookBookBlockReadingRecord",
      summary = "Set reading disposition for a book block")
  @PutMapping("/{notebook}/book/blocks/{bookBlock}/reading-record")
  @Transactional
  public List<BookBlockReadingRecordListItem> putBlockReadingRecord(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("bookBlock") @Schema(type = "integer") BookBlock bookBlock,
      @RequestBody(required = false) @Valid BookBlockReadingRecordPutRequest body)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(notebook);
    var user = authorizationService.getCurrentUser();
    String status = body == null ? BookBlockReadingRecord.STATUS_READ : body.getStatus();
    bookService.upsertReadingRecord(notebook, user, bookBlock, status);
    return bookService.listReadingRecordsForBook(notebook, user);
  }

  @Operation(
      operationId = "deleteNotebookBookBlockReadingRecord",
      summary = "Clear reading disposition for a book block")
  @DeleteMapping("/{notebook}/book/blocks/{bookBlock}/reading-record")
  @Transactional
  public List<BookBlockReadingRecordListItem> deleteBlockReadingRecord(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("bookBlock") @Schema(type = "integer") BookBlock bookBlock)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(notebook);
    var user = authorizationService.getCurrentUser();
    bookService.deleteReadingRecord(notebook, user, bookBlock);
    return bookService.listReadingRecordsForBook(notebook, user);
  }
}
