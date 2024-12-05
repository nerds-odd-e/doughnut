package com.odde.doughnut.services.impl;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GraphRAGService;
import com.odde.doughnut.services.graphRAG.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GraphRAGServiceImpl implements GraphRAGService {
  private String formatUriAndTitle(Note note) {
    return String.format("[%s](/n%d)", note.getTopicConstructor(), note.getId());
  }

  private List<String> buildContextualPath(Note note) {
    if (note.getParent() == null) {
      return Collections.emptyList();
    }
    return Collections.singletonList(formatUriAndTitle(note.getParent()));
  }

  @Override
  public GraphRAGResult retrieve(Note focusNote, int tokenBudget) {
    String uriAndTitle = formatUriAndTitle(focusNote);
    String detailsTruncated = focusNote.getNoteTopology().getShortDetails();
    String parentUriAndTitle =
        focusNote.getParent() != null ? formatUriAndTitle(focusNote.getParent()) : null;

    FocusNote focus =
        new FocusNote(
            uriAndTitle,
            detailsTruncated,
            parentUriAndTitle,
            null,
            buildContextualPath(focusNote),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList());

    return new GraphRAGResult(focus, new ArrayList<>());
  }
}
