package com.odde.donut.services.book;

import com.odde.donut.controllers.dto.BookBlockMutationResponse;
import com.odde.donut.controllers.dto.BookMutationResponse;
import com.odde.donut.entities.Book;
import java.util.List;
import java.util.Set;

public final class BookMutationResponseMapper {

  private BookMutationResponseMapper() {}

  public static BookMutationResponse fromBook(
      Book book, Set<Integer> blockIdsIncludingUpdatedLocators) {
    List<BookBlockMutationResponse> blocks =
        book.getBlocks().stream()
            .map(
                b ->
                    new BookBlockMutationResponse(
                        b.getId(),
                        b.getDepth(),
                        b.getStructuralTitle(),
                        blockIdsIncludingUpdatedLocators.contains(b.getId())
                            ? b.getContentLocators()
                            : null))
            .toList();
    Integer notebookId = book.getNotebookId();
    return new BookMutationResponse(
        book.getId(),
        book.getBookName(),
        book.getFormat(),
        book.getCreatedAt(),
        book.getUpdatedAt(),
        blocks,
        notebookId == null ? null : notebookId.toString());
  }
}
