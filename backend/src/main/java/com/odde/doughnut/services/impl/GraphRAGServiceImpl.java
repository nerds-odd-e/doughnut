package com.odde.doughnut.services.impl;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GraphRAGService;
import com.odde.doughnut.services.graphRAG.*;
import java.util.ArrayList;
import java.util.Collections;
import org.springframework.stereotype.Service;

@Service
public class GraphRAGServiceImpl implements GraphRAGService {
  @Override
  public GraphRAGResult retrieve(Note focusNote, int tokenBudget) {
    String uriAndTitle =
        String.format("[%s](/n%d)", focusNote.getTopicConstructor(), focusNote.getId());
    String detailsTruncated = focusNote.getNoteTopology().getShortDetails();

    FocusNote focus =
        new FocusNote(
            uriAndTitle,
            detailsTruncated,
            null, // parentUriAndTitle
            null, // objectUriAndTitle
            Collections.emptyList(), // contextualPath
            Collections.emptyList(), // children
            Collections.emptyList(), // referrings
            Collections.emptyList(), // priorSiblings
            Collections.emptyList() // youngerSiblings
            );

    return new GraphRAGResult(focus, new ArrayList<>());
  }
}
