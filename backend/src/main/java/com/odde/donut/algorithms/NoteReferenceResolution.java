package com.odde.donut.algorithms;

import com.odde.donut.entities.Note;

/**
 * Resolution of an {@link AuthoredNoteReference} against a source note's scope and a viewer's
 * current readability: exactly one readable match ({@link Resolved}), no readable match ({@link
 * Missing}), or more than one readable match ({@link Ambiguous}).
 */
public sealed interface NoteReferenceResolution
    permits NoteReferenceResolution.Resolved,
        NoteReferenceResolution.Missing,
        NoteReferenceResolution.Ambiguous {

  record Resolved(Note destinationNote) implements NoteReferenceResolution {}

  record Missing() implements NoteReferenceResolution {}

  record Ambiguous() implements NoteReferenceResolution {}
}
