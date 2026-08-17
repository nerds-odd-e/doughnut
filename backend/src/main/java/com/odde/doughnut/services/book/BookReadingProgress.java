package com.odde.doughnut.services.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.BookBlockReadingRecordListItem;
import com.odde.doughnut.controllers.dto.BookLastReadPositionRequest;
import com.odde.doughnut.controllers.dto.BookUserLastReadPositionResponse;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookBlock;
import com.odde.doughnut.entities.BookBlockReadingRecord;
import com.odde.doughnut.entities.BookUserLastReadPosition;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.BookBlockReadingRecordRepository;
import com.odde.doughnut.entities.repositories.BookBlockRepository;
import com.odde.doughnut.entities.repositories.BookUserLastReadPositionRepository;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.testability.TestabilitySettings;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class BookReadingProgress {

  private final BookUserLastReadPositionRepository bookUserLastReadPositionRepository;
  private final BookBlockRepository bookBlockRepository;
  private final BookBlockReadingRecordRepository bookBlockReadingRecordRepository;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final ObjectMapper objectMapper;

  BookReadingProgress(
      BookUserLastReadPositionRepository bookUserLastReadPositionRepository,
      BookBlockRepository bookBlockRepository,
      BookBlockReadingRecordRepository bookBlockReadingRecordRepository,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      ObjectMapper objectMapper) {
    this.bookUserLastReadPositionRepository = bookUserLastReadPositionRepository;
    this.bookBlockRepository = bookBlockRepository;
    this.bookBlockReadingRecordRepository = bookBlockReadingRecordRepository;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.objectMapper = objectMapper;
  }

  List<BookBlockReadingRecordListItem> list(Book book, User user) {
    return bookBlockReadingRecordRepository
        .findAllByUser_IdAndBookBlock_Book_Id(user.getId(), book.getId())
        .stream()
        .map(
            row ->
                new BookBlockReadingRecordListItem(
                    row.getBookBlock().getId(), row.getStatus(), row.getCompletedAt()))
        .toList();
  }

  Optional<BookUserLastReadPositionResponse> getLastReadPosition(Book book, User user) {
    return bookUserLastReadPositionRepository
        .findByUser_IdAndBook_Id(user.getId(), book.getId())
        .map(row -> BookUserLastReadPositionResponse.from(row, objectMapper));
  }

  void upsertLastReadPosition(Book book, User user, BookLastReadPositionRequest request) {
    final Optional<BookBlock> selectedBlockPatch =
        request.getSelectedBookBlockId() == null
            ? Optional.empty()
            : Optional.of(resolveBookBlockForBook(request.getSelectedBookBlockId(), book));
    bookUserLastReadPositionRepository
        .findByUser_IdAndBook_Id(user.getId(), book.getId())
        .map(
            existing -> {
              applyReadingPositionFields(existing, request);
              selectedBlockPatch.ifPresent(existing::setSelectedBookBlock);
              return entityPersister.save(existing);
            })
        .orElseGet(
            () -> {
              var row = new BookUserLastReadPosition();
              row.setUser(user);
              row.setBook(book);
              applyReadingPositionFields(row, request);
              selectedBlockPatch.ifPresent(row::setSelectedBookBlock);
              return entityPersister.save(row);
            });
    entityPersister.flush();
  }

  void upsertReadingRecord(Book book, User user, BookBlock bookBlock, String status) {
    if (!(BookBlockReadingRecord.STATUS_READ.equals(status)
        || BookBlockReadingRecord.STATUS_SKIMMED.equals(status)
        || BookBlockReadingRecord.STATUS_SKIPPED.equals(status))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reading record status");
    }
    if (!bookBlock.getBook().getId().equals(book.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
    }
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    bookBlockReadingRecordRepository
        .findByUser_IdAndBookBlock_Id(user.getId(), bookBlock.getId())
        .map(
            existing -> {
              existing.setStatus(status);
              existing.setCompletedAt(now);
              return entityPersister.save(existing);
            })
        .orElseGet(
            () -> {
              var row = new BookBlockReadingRecord();
              row.setUser(user);
              row.setBookBlock(bookBlock);
              row.setStatus(status);
              row.setCompletedAt(now);
              return entityPersister.save(row);
            });
    entityPersister.flush();
  }

  private void applyReadingPositionFields(
      BookUserLastReadPosition row, BookLastReadPositionRequest request) {
    if (request.getLocator() != null) {
      BookFormat.forLocator(request.getLocator())
          .writeReadingPositionLocator(row, request.getLocator(), objectMapper);
      return;
    }
    String existing = row.getReadingPositionLocatorJson();
    if (existing == null || existing.isBlank()) {
      throw new ApiException(
          "reading-position patch requires locator when none is stored yet",
          ApiError.ErrorType.BINDING_ERROR,
          "reading-position patch requires locator when none is stored yet");
    }
  }

  private BookBlock resolveBookBlockForBook(int bookBlockId, Book book) {
    BookBlock bb =
        bookBlockRepository
            .findById(bookBlockId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
    if (!bb.getBook().getId().equals(book.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
    }
    return bb;
  }
}
