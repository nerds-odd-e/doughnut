package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteReference;
import com.odde.donut.algorithms.AuthoredNoteReferences;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.algorithms.NoteReferenceResolution;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.AuthoredNoteReferenceInboundFacade;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.CRC32;
import org.springframework.stereotype.Service;

/**
 * Application facade for live note-reference resolution: outgoing wiki links, inbound referrers,
 * focus-context sampling, and derived-index refresh after content mutations. Consumers outside the
 * authored-reference persistence package use this type — not {@link
 * AuthoredNoteReferenceInboundFacade} or its row repository.
 */
@Service
public class NoteReferenceService {

  @PersistenceContext private EntityManager entityManager;

  private final WikiLinkResolver wikiLinkResolver;
  private final CanonicalDonutOrigin canonicalDonutOrigin;
  private final AuthoredNoteReferenceInboundFacade authoredNoteReferenceInboundFacade;
  private final NotePropertyIndexService notePropertyIndexService;
  private final NoteAliasIndexService noteAliasIndexService;
  private final NoteLevelIndexService noteLevelIndexService;

  public NoteReferenceService(
      WikiLinkResolver wikiLinkResolver,
      CanonicalDonutOrigin canonicalDonutOrigin,
      AuthoredNoteReferenceInboundFacade authoredNoteReferenceInboundFacade,
      NotePropertyIndexService notePropertyIndexService,
      NoteAliasIndexService noteAliasIndexService,
      NoteLevelIndexService noteLevelIndexService) {
    this.wikiLinkResolver = wikiLinkResolver;
    this.canonicalDonutOrigin = canonicalDonutOrigin;
    this.authoredNoteReferenceInboundFacade = authoredNoteReferenceInboundFacade;
    this.notePropertyIndexService = notePropertyIndexService;
    this.noteAliasIndexService = noteAliasIndexService;
    this.noteLevelIndexService = noteLevelIndexService;
  }

  public List<WikiLink> wikiLinksForViewer(Note focusNote, User viewer) {
    String content = focusNote.getContent();
    if (content == null || content.isBlank()) {
      return List.of();
    }
    List<WikiLink> out = new ArrayList<>();
    for (AuthoredNoteReference ref :
        AuthoredNoteReferences.uniquePreserveOrder(
            AuthoredNoteReferences.inOccurrenceOrder(content, canonicalDonutOrigin))) {
      WikiLink wikiLink = wikiLinkFor(ref, focusNote, viewer);
      if (wikiLink != null) {
        out.add(wikiLink);
      }
    }
    return List.copyOf(out);
  }

  /**
   * Resolves {@code ref} against {@code focusNote}'s scope and {@code viewer}'s current
   * readability, live: {@code null} when missing (excluded from output), a resolved {@link
   * WikiLink} when there's exactly one readable match, an ambiguous {@link WikiLink} when there's
   * more than one.
   */
  private WikiLink wikiLinkFor(AuthoredNoteReference ref, Note focusNote, User viewer) {
    return switch (wikiLinkResolver.resolveReference(ref, focusNote, viewer)) {
      case NoteReferenceResolution.Resolved resolved ->
          WikiLinks.resolved(ref, resolved.destinationNote().getId());
      case NoteReferenceResolution.Missing ignored -> null;
      case NoteReferenceResolution.Ambiguous ignored -> WikiLinks.ambiguous(ref);
    };
  }

  /**
   * Authorized outgoing note-reference target notes for viewer (same authorization as {@link
   * #wikiLinksForViewer}). Each target note appears once: multiple resolved links that share the
   * same target token (with different display text) still yield one outgoing note for graph-style
   * consumers; {@link #wikiLinksForViewer} retains one entry per distinct stored link text.
   */
  public List<Note> outgoingWikiLinkTargetNotesForViewer(Note focusNote, User viewer) {
    List<Note> notes = new ArrayList<>();
    Set<Integer> seenTargetIds = new LinkedHashSet<>();
    for (WikiLink wt : wikiLinksForViewer(focusNote, viewer)) {
      Integer id = wt.getDestinationNoteId();
      if (id == null || !seenTargetIds.add(id)) {
        continue;
      }
      Note n = entityManager.find(Note.class, id);
      if (n != null) {
        notes.add(n);
      }
    }
    return List.copyOf(notes);
  }

  /**
   * Distinct referrer notes whose authored reference live-resolves to {@code target} for {@code
   * viewer}, ordered by referrer note id ascending.
   */
  public List<Note> distinctReferrerNotesForViewer(Note target, User viewer) {
    return authoredNoteReferenceInboundFacade.distinctReferrerNotesForViewer(target, viewer);
  }

  /**
   * One referrer note plus the distinct authored link text(s) it uses to refer to {@code target}.
   *
   * @see AuthoredNoteReferenceInboundFacade#distinctInboundReferencesForViewer
   */
  public List<AuthoredNoteReferenceInboundFacade.InboundReference>
      distinctInboundReferencesForViewer(Note target, User viewer) {
    return authoredNoteReferenceInboundFacade.distinctInboundReferencesForViewer(target, viewer);
  }

  /**
   * Inbound referrers for focus-context only, with authored-note-reference visibility (referrer's
   * notebook vs the focal notebook and {@link User#canReferTo}), distinct by referrer id, excluding
   * {@code excludeNoteIds}, capped in memory after sampling.
   */
  public List<Note> sampledReferencesNotesForFocusContext(
      Note focalNote,
      User viewer,
      Set<Integer> excludeNoteIds,
      int cap,
      Optional<Long> sampleSeed) {
    if (cap <= 0 || focalNote.getId() == null) {
      return List.of();
    }
    List<Note> candidates = new ArrayList<>();
    for (Note referrer : distinctReferrerNotesForViewer(focalNote, viewer)) {
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

  /** Rebuilds property, alias, and level derived indexes for {@code note}. */
  public void refreshDerivedIndexesForNote(Note note) {
    notePropertyIndexService.refreshForNote(note);
    noteAliasIndexService.refreshForNote(note);
    noteLevelIndexService.refreshForNote(note);
  }

  /** Replicates MySQL's {@code CRC32(CONCAT(CAST(id AS CHAR), CAST(seed AS CHAR)))}. */
  private static long crc32(int noteId, long seed) {
    CRC32 crc32 = new CRC32();
    crc32.update((Integer.toString(noteId) + Long.toString(seed)).getBytes(StandardCharsets.UTF_8));
    return crc32.getValue();
  }
}
