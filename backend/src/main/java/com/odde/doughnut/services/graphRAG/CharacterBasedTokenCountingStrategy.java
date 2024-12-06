package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;

public class CharacterBasedTokenCountingStrategy implements TokenCountingStrategy {
  @Override
  public int estimateTokens(Note note) {
    int detailsLength =
        note.getDetails() != null
            ? Math.min(
                note.getDetails().length(), GraphRAGConstants.RELATED_NOTE_DETAILS_TRUNCATE_LENGTH)
            : 0;
    int titleLength = note.getTopicConstructor().length();
    return (int) Math.ceil((detailsLength + titleLength) / GraphRAGConstants.CHARACTERS_PER_TOKEN);
  }
}
