package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookBlock;

abstract class NotebookBooksBlockControllerTestBase extends NotebookBooksControllerTestBase {

  static BookBlock blockByTitle(Book book, String title) {
    return book.getBlocks().stream()
        .filter(b -> b.getStructuralTitle().equals(title))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Block not found: " + title));
  }
}
