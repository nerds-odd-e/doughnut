package com.odde.doughnut.services.impl;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class GraphRAGServiceImpl {
  public static final int RELATED_NOTE_DETAILS_TRUNCATE_LENGTH = 1000;
  public static final double CHARACTERS_PER_TOKEN = 3.75;

  private String formatUriAndTitle(Note note) {
    return String.format("[%s](%s)", note.getTopicConstructor(), note.getUri());
  }

  private String truncateDetails(String details) {
    if (details == null || details.length() <= RELATED_NOTE_DETAILS_TRUNCATE_LENGTH) {
      return details;
    }
    return details.substring(0, RELATED_NOTE_DETAILS_TRUNCATE_LENGTH);
  }

  private List<String> buildContextualPath(Note note) {
    return note.getAncestors().stream().map(this::formatUriAndTitle).collect(Collectors.toList());
  }

  private BareNote createRelatedNote(Note note) {
    return new BareNote(formatUriAndTitle(note), truncateDetails(note.getDetails()), null, null);
  }

  private int estimateTokens(BareNote note) {
    String jsonString = defaultObjectMapper().valueToTree(note).toString();
    return (int) Math.ceil(jsonString.length() / CHARACTERS_PER_TOKEN);
  }

  public GraphRAGResult retrieve(Note focusNote, int tokenBudget) {
    String uriAndTitle = formatUriAndTitle(focusNote);
    String detailsTruncated = focusNote.getDetails();
    String parentUriAndTitle =
        focusNote.getParent() != null ? formatUriAndTitle(focusNote.getParent()) : null;
    String objectUriAndTitle =
        focusNote.getTargetNote() != null ? formatUriAndTitle(focusNote.getTargetNote()) : null;

    // Calculate children that fit within budget
    List<Note> childrenWithinBudget = new ArrayList<>();
    int remainingTokens = tokenBudget;
    if (remainingTokens > 0) {
      for (Note child : focusNote.getChildren()) {
        BareNote childNote = createRelatedNote(child);
        int childTokens = estimateTokens(childNote);
        if (childTokens <= remainingTokens) {
          childrenWithinBudget.add(child);
          remainingTokens -= childTokens;
        }
      }
    }

    FocusNote focus =
        new FocusNote(
            uriAndTitle,
            detailsTruncated,
            parentUriAndTitle,
            objectUriAndTitle,
            buildContextualPath(focusNote),
            childrenWithinBudget.stream().map(this::formatUriAndTitle).collect(Collectors.toList()),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList());

    List<BareNote> relatedNotes = new ArrayList<>();
    // Add ancestors to related notes (Priority 1)
    for (Note ancestor : focusNote.getAncestors()) {
      relatedNotes.add(createRelatedNote(ancestor));
    }
    // Add object note to related notes (Priority 1)
    if (focusNote.getTargetNote() != null) {
      Note target = focusNote.getTargetNote();
      relatedNotes.add(createRelatedNote(target));
      // Add object's ancestors to related notes (Priority 1)
      for (Note ancestor : target.getAncestors()) {
        relatedNotes.add(createRelatedNote(ancestor));
      }
    }
    // Add children to related notes (Priority 2)
    for (Note child : childrenWithinBudget) {
      relatedNotes.add(createRelatedNote(child));
    }

    return new GraphRAGResult(focus, relatedNotes);
  }
}
