package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;

/**
 * Handlers that yield a sequence of related notes for {@link
 * com.odde.doughnut.services.graphRAG.PriorityLayer}.
 */
public interface PollableNoteRelationshipHandler {
  Note pollNext();
}
