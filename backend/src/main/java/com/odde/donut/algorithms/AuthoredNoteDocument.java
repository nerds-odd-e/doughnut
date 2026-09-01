package com.odde.donut.algorithms;

import java.util.List;

/**
 * Validated Markdown ready to persist as a note's content, paired with the authored note references
 * parsed from that same content ({@link AuthoredNoteReferences#uniquePreserveOrder}). Produced once
 * per save so the Markdown and its references never drift apart; consumed by {@code
 * Note.replaceContent} to update both in the same aggregate operation.
 */
public record AuthoredNoteDocument(
    String validatedMarkdown, List<AuthoredNoteReference> references) {}
