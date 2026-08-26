package com.odde.donut.controllers;

import com.odde.donut.entities.Book;
import com.odde.donut.entities.BookBlock;

abstract class NotebookBooksBlockControllerTestBase extends NotebookBooksControllerTestBase {

  static BookBlock blockByTitle(Book book, String title) {
    return book.getBlocks().stream()
        .filter(b -> b.getStructuralTitle().equals(title))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Block not found: " + title));
  }
}
