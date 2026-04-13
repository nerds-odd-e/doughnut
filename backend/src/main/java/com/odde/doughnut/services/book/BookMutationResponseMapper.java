package com.odde.doughnut.services.book;

import com.odde.doughnut.controllers.dto.BookBlockMutationResponse;
import com.odde.doughnut.controllers.dto.BookMutationResponse;
import com.odde.doughnut.entities.Book;
import java.util.List;
import java.util.Set;

public final class BookMutationResponseMapper {

  private BookMutationResponseMapper() {}

  public static BookMutationResponse fromBook(Book book, Set<Integer> blockIdsIncludingAllBboxes) {
    List<BookBlockMutationResponse> blocks =
        book.getBlocks().stream()
            .map(
                b ->
                    new BookBlockMutationResponse(
                        b.getId(),
                        b.getDepth(),
                        b.getStructuralTitle(),
                        blockIdsIncludingAllBboxes.contains(b.getId()) ? b.getAllBboxes() : null))
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
