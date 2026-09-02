package com.odde.donut.services;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.AuthoredNoteReferenceInboundFacade;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.CRC32;

/** Inbound authored-note-reference focus-context sampling for a focal note. */
final class InboundResolvedWikiLinks {

  private final AuthoredNoteReferenceInboundFacade authoredNoteReferenceInboundFacade;

  InboundResolvedWikiLinks(AuthoredNoteReferenceInboundFacade authoredNoteReferenceInboundFacade) {
    this.authoredNoteReferenceInboundFacade = authoredNoteReferenceInboundFacade;
  }

  List<Note> sampledReferencesNotesForFocusContext(
      Note focalNote,
      User viewer,
      Set<Integer> excludeNoteIds,
      int cap,
      Optional<Long> sampleSeed) {
    if (cap <= 0 || focalNote.getId() == null) {
      return List.of();
    }
    List<Note> candidates = new ArrayList<>();
    for (Note referrer :
        authoredNoteReferenceInboundFacade.distinctReferrerNotesForViewer(focalNote, viewer)) {
      if (!excludeNoteIds.contains(referrer.getId())) {
        candidates.add(referrer);
      }
    }
    sampleSeed.ifPresent(
        seed -> candidates.sort(Comparator.comparingLong(note -> crc32(note.getId(), seed))));
    return candidates.size() <= cap
        ? List.copyOf(candidates)
        : List.copyOf(candidates.subList(0, cap));
  }

  /** Replicates MySQL's {@code CRC32(CONCAT(CAST(id AS CHAR), CAST(seed AS CHAR)))}. */
  private static long crc32(int noteId, long seed) {
    CRC32 crc32 = new CRC32();
    crc32.update((Integer.toString(noteId) + Long.toString(seed)).getBytes(StandardCharsets.UTF_8));
    return crc32.getValue();
  }
}
