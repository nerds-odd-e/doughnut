package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.BookLayoutReorganizationSuggestion;
import com.odde.donut.controllers.dto.BookLayoutReorganizationSuggestion.BlockDepthSuggestion;
import com.odde.donut.entities.BookBlock;
import com.odde.donut.entities.Notebook;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract class NotebookBooksLayoutReorganizationControllerTestBase
    extends NotebookBooksBlockControllerTestBase {

  BookLayoutReorganizationSuggestion suggestionWithDepths(
      Notebook notebook, Map<String, Integer> depthByTitle) {
    var suggestion = new BookLayoutReorganizationSuggestion();
    List<BlockDepthSuggestion> items = new ArrayList<>();
    for (BookBlock b : bookOf(notebook).getBlocks()) {
      var e = new BlockDepthSuggestion();
      e.setId(b.getId());
      e.setDepth(depthByTitle.get(b.getStructuralTitle()));
      items.add(e);
    }
    suggestion.setBlocks(items);
    return suggestion;
  }

  Map<String, Integer> nestBAndCDepths() {
    return Map.of("A", 0, "B", 1, "C", 2, "D", 0);
  }
}
