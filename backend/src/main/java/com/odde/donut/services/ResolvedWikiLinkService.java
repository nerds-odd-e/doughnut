package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteReference;
import com.odde.donut.algorithms.AuthoredNoteReferences;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.algorithms.NoteReferenceResolution;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.ResolvedWikiLink;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.entities.repositories.ResolvedWikiLinkRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResolvedWikiLinkService {

  @PersistenceContext private EntityManager entityManager;

  private final WikiLinkResolver wikiLinkResolver;
  private final ResolvedWikiLinkRepository resolvedWikiLinkRepository;
  private final ResolvedWikiLinkRefresh resolvedWikiLinkRefresh;
  private final CanonicalDonutOrigin canonicalDonutOrigin;

  public ResolvedWikiLinkService(
      WikiLinkResolver wikiLinkResolver,
      ResolvedWikiLinkRepository resolvedWikiLinkRepository,
      NotePropertyIndexService notePropertyIndexService,
      NoteAliasIndexService noteAliasIndexService,
      NoteLevelIndexService noteLevelIndexService,
      NoteRepository noteRepository,
      CanonicalDonutOrigin canonicalDonutOrigin) {
    this.wikiLinkResolver = wikiLinkResolver;
    this.resolvedWikiLinkRepository = resolvedWikiLinkRepository;
    this.canonicalDonutOrigin = canonicalDonutOrigin;
    this.resolvedWikiLinkRefresh =
        new ResolvedWikiLinkRefresh(
            wikiLinkResolver,
            resolvedWikiLinkRepository,
            notePropertyIndexService,
            noteAliasIndexService,
            noteLevelIndexService,
            noteRepository);
  }

  private InboundResolvedWikiLinks inbound() {
    return new InboundResolvedWikiLinks(resolvedWikiLinkRepository);
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
   * readability, live (not from a cached {@link ResolvedWikiLink} row): {@code null} when missing
   * (excluded from output), a resolved {@link WikiLink} when there's exactly one readable match, an
   * ambiguous {@link WikiLink} when there's more than one.
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
   * Authorized outgoing wiki-link target notes for viewer (same authorization as {@link
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
   * True when at least one non-deleted note has a resolved wiki-link row pointing at {@code
   * targetNoteId}. Used to require an explicit reference-handling choice on title rename.
   */
  public boolean hasInboundResolvedWikiLinkRowsFromNonDeletedReferrers(Integer targetNoteId) {
    return inbound().hasRowsFromNonDeletedReferrers(targetNoteId);
  }

  /**
   * Inbound referrers for focus-context only, with resolved-wiki-link visibility (referrer's
   * notebook vs the focal notebook and {@link User#canReferTo}), distinct by referrer id, excluding
   * {@code excludeNoteIds}, capped in the database.
   */
  public List<Note> sampledReferencesNotesForFocusContext(
      Note focalNote,
      User viewer,
      Set<Integer> excludeNoteIds,
      int cap,
      Optional<Long> sampleSeed) {
    return inbound()
        .sampledReferencesNotesForFocusContext(focalNote, viewer, excludeNoteIds, cap, sampleSeed);
  }

  /** Note-local: property/alias/level indexes, outgoing resolution rows, property-link validity. */
  @Transactional
  public void refreshForNote(Note note, User viewer) {
    resolvedWikiLinkRefresh.refreshForNote(entityManager, note, viewer);
  }

  /**
   * Notebook resolution scope only: outgoing resolution rows and property-link validity for live
   * notes. Does not rebuild derived indexes; call {@link #refreshForNote} first when the changed
   * note's indexes must be current before other notes re-resolve.
   */
  @Transactional
  public void refreshNotebookScope(Notebook notebook, User viewer) {
    resolvedWikiLinkRefresh.refreshNotebookScope(entityManager, notebook, viewer);
  }

  /**
   * Content changing notebooks can add or remove a title/alias candidate from either notebook's
   * Portable-path resolution scope. Re-resolve both. No-op when the move stayed in the same
   * notebook.
   */
  @Transactional
  public void refreshCardinalityAcrossMovedNotebooks(
      Notebook sourceNotebook, Notebook destinationNotebook, User viewer) {
    Integer sourceNotebookId = sourceNotebook != null ? sourceNotebook.getId() : null;
    if (Objects.equals(sourceNotebookId, destinationNotebook.getId())) {
      return;
    }
    if (sourceNotebook != null) {
      refreshNotebookScope(sourceNotebook, viewer);
    }
    refreshNotebookScope(destinationNotebook, viewer);
  }
}
