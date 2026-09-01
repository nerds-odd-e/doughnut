package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteReference;
import com.odde.donut.algorithms.AuthoredNoteReferences;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.algorithms.WikiLinkPropertyMatch;
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
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResolvedWikiLinkService {

  @PersistenceContext private EntityManager entityManager;

  private final AmbiguousWikiLinks ambiguousWikiLinks;
  private final ResolvedWikiLinkRepository resolvedWikiLinkRepository;
  private final AuthorizationService authorizationService;
  private final ResolvedWikiLinkRefresh resolvedWikiLinkRefresh;
  private final CanonicalDonutOrigin canonicalDonutOrigin;

  public ResolvedWikiLinkService(
      WikiLinkResolver wikiLinkResolver,
      ResolvedWikiLinkRepository resolvedWikiLinkRepository,
      AuthorizationService authorizationService,
      NotePropertyIndexService notePropertyIndexService,
      NoteAliasIndexService noteAliasIndexService,
      NoteLevelIndexService noteLevelIndexService,
      NoteRepository noteRepository,
      CanonicalDonutOrigin canonicalDonutOrigin) {
    this.ambiguousWikiLinks = new AmbiguousWikiLinks(wikiLinkResolver);
    this.resolvedWikiLinkRepository = resolvedWikiLinkRepository;
    this.authorizationService = authorizationService;
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
    return new InboundResolvedWikiLinks(resolvedWikiLinkRepository, entityManager);
  }

  public List<WikiLink> wikiLinksForViewer(Note focusNote, User viewer) {
    List<WikiLink> out = new ArrayList<>();
    Set<String> emittedAuthored = new LinkedHashSet<>();
    for (ResolvedWikiLink row :
        resolvedWikiLinkRepository.findBySourceNote_IdOrderByIdAsc(focusNote.getId())) {
      Note resolved = authorizedOutgoingTargetNote(focusNote, row, viewer);
      if (resolved != null) {
        out.add(
            WikiLinks.resolvedFromStoredAuthoredLink(
                row.getAuthoredLink(), resolved.getId(), canonicalDonutOrigin));
        emittedAuthored.add(row.getAuthoredLink());
      }
    }
    out.addAll(ambiguousWikiLinks.forAuthoredTokensNotIn(focusNote, viewer, emittedAuthored));
    return List.copyOf(out);
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

  private Note authorizedOutgoingTargetNote(Note sourceNoteRef, ResolvedWikiLink row, User viewer) {
    Note target = row.getDestinationNote();
    if (target.getDeletedAt() != null) {
      return null;
    }
    Notebook notebook =
        target.getNotebook() != null ? target.getNotebook() : sourceNoteRef.getNotebook();
    if (!authorizationService.userMayReadNotebook(viewer, notebook)) {
      return null;
    }
    Note resolved = entityManager.find(Note.class, target.getId());
    if (resolved == null) {
      return null;
    }
    return switch (AuthoredNoteReferences.fromStoredAuthoredLink(
        row.getAuthoredLink(), canonicalDonutOrigin)) {
      case AuthoredNoteReference.NoteIdUrlTarget ignored -> resolved;
      case AuthoredNoteReference.WikiPortablePathTarget ignored ->
          WikiLinkPropertyMatch.matchesTargetNoteContent(
                  row.getAuthoredLink(), resolved.getContent())
              ? resolved
              : null;
    };
  }

  /**
   * Notes whose resolved wiki links point at {@code focalNote}, for {@link
   * com.odde.donut.controllers.dto.NoteRealm} inbound references. Visibility uses the referrer's
   * notebook vs the focal notebook and {@link User#canReferTo}.
   */
  public List<Note> inboundReferrerNotesForViewer(Note focalNote, User viewer) {
    return inbound().referrerNotesForViewer(focalNote, viewer);
  }

  /**
   * Referrer notes for {@code focalNote} and {@code viewer}: all resolved wiki-link inbound links
   * ({@link #inboundReferrerNotesForViewer}), ordered by note id for {@link
   * com.odde.donut.controllers.dto.NoteRealm#getReferences()} (as topologies) and focus context
   * retrieval.
   */
  public List<Note> referencesNotesForViewer(Note focalNote, User viewer) {
    return inbound().referencesNotesForViewer(focalNote, viewer);
  }

  /**
   * True when at least one non-deleted note has a resolved wiki-link row pointing at {@code
   * targetNoteId}. Used to require an explicit reference-handling choice on title rename.
   */
  public boolean hasInboundResolvedWikiLinkRowsFromNonDeletedReferrers(Integer targetNoteId) {
    return inbound().hasRowsFromNonDeletedReferrers(targetNoteId);
  }

  /**
   * Inbound referrers for focus-context only: same visibility as {@link #referencesNotesForViewer},
   * distinct by referrer id, excluding {@code excludeNoteIds}, capped in the database.
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

  @Transactional
  public void refreshForNote(Note note, User viewer) {
    resolvedWikiLinkRefresh.refreshForNote(entityManager, note, viewer);
  }

  @Transactional
  public void refreshNotebookScope(Notebook notebook, User viewer) {
    resolvedWikiLinkRefresh.refreshNotebookScope(entityManager, notebook, viewer);
  }
}
