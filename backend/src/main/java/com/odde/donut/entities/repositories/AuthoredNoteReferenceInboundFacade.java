package com.odde.donut.entities.repositories;

import com.odde.donut.algorithms.AuthoredNoteReference;
import com.odde.donut.algorithms.FrontmatterAliases;
import com.odde.donut.algorithms.NoteReferenceResolution;
import com.odde.donut.algorithms.PathShapedTarget;
import com.odde.donut.controllers.dto.FolderTrailSegments;
import com.odde.donut.entities.AuthoredNoteReferenceRow;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.services.WikiLinkResolver;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Live inbound-candidate resolution against {@code authored_note_reference} rows (ADR 0001 Wiki
 * link). Selects candidate rows by the target note's <em>current</em> addressable keys (its
 * authored note ID, and its current wiki notebook/title/alias/Portable-path identity — never
 * anything cached on the row itself), then re-resolves each candidate through {@link
 * WikiLinkResolver#resolveReference} for the viewer before returning only the rows that actually
 * resolve back to the target. Candidate lookup is an optimization, not a resolution verdict: rows
 * can go stale as content, titles, or aliases change elsewhere, so every candidate is re-verified
 * live.
 *
 * <p>Exposes only domain-shaped results ({@link Note}) to callers, never the internal {@link
 * AuthoredNoteReferenceRow} persistence rows — see {@link AuthoredNoteReferenceRowRepository}.
 */
@Service
public class AuthoredNoteReferenceInboundFacade {

  private final AuthoredNoteReferenceRowRepository authoredNoteReferenceRowRepository;
  private final NoteAliasIndexRepository noteAliasIndexRepository;
  private final WikiLinkResolver wikiLinkResolver;

  public AuthoredNoteReferenceInboundFacade(
      AuthoredNoteReferenceRowRepository authoredNoteReferenceRowRepository,
      NoteAliasIndexRepository noteAliasIndexRepository,
      WikiLinkResolver wikiLinkResolver) {
    this.authoredNoteReferenceRowRepository = authoredNoteReferenceRowRepository;
    this.noteAliasIndexRepository = noteAliasIndexRepository;
    this.wikiLinkResolver = wikiLinkResolver;
  }

  /**
   * Distinct referrer notes whose authored reference live-resolves to {@code target} for {@code
   * viewer}, ordered by referrer note id ascending — the same deterministic order {@code
   * ResolvedWikiLinkService}'s inbound methods guarantee today.
   */
  public List<Note> distinctReferrerNotesForViewer(Note target, User viewer) {
    LinkedHashMap<Integer, Note> distinctReferrersInOrder = new LinkedHashMap<>();
    for (AuthoredNoteReferenceRow candidate : candidateRowsForTarget(target)) {
      Note sourceNote = candidate.getNote();
      Integer sourceNoteId = sourceNote.getId();
      if (distinctReferrersInOrder.containsKey(sourceNoteId)) {
        continue;
      }
      if (resolvesToTarget(candidate, sourceNote, target, viewer)
          && referrerVisibleToViewer(sourceNote, target, viewer)) {
        distinctReferrersInOrder.put(sourceNoteId, sourceNote);
      }
    }
    return List.copyOf(distinctReferrersInOrder.values());
  }

  /**
   * A soft-deleted referrer is never inbound, regardless of viewer. Otherwise, checks the
   * referrer's own visibility (distinct from {@link #resolvesToTarget}'s target-side readability
   * check): same notebook as {@code target} is always visible; a different notebook requires {@code
   * viewer} to own or subscribe to the referrer's own notebook ({@link User#canReferTo}).
   */
  private boolean referrerVisibleToViewer(Note sourceNote, Note target, User viewer) {
    if (sourceNote.getDeletedAt() != null) {
      return false;
    }
    Notebook referrerNotebook = sourceNote.getNotebook();
    Notebook targetNotebook = target.getNotebook();
    if (referrerNotebook != null
        && targetNotebook != null
        && referrerNotebook.getId().equals(targetNotebook.getId())) {
      return true;
    }
    if (viewer == null || referrerNotebook == null) {
      return false;
    }
    return viewer.canReferTo(referrerNotebook);
  }

  private boolean resolvesToTarget(
      AuthoredNoteReferenceRow candidate, Note sourceNote, Note target, User viewer) {
    AuthoredNoteReference reference = candidate.toDomainReference();
    NoteReferenceResolution resolution =
        wikiLinkResolver.resolveReference(reference, sourceNote, viewer);
    return resolution instanceof NoteReferenceResolution.Resolved resolved
        && resolved.destinationNote().getId().equals(target.getId());
  }

  private List<AuthoredNoteReferenceRow> candidateRowsForTarget(Note target) {
    List<AuthoredNoteReferenceRow> candidates = new ArrayList<>();
    candidates.addAll(
        authoredNoteReferenceRowRepository.findNoteIdUrlCandidatesForTarget(
            AuthoredNoteReferenceRow.Kind.NOTE_ID_URL, target.getId()));
    candidates.addAll(wikiCandidateRowsForTarget(target));
    candidates.sort(
        Comparator.<AuthoredNoteReferenceRow, Integer>comparing(row -> row.getNote().getId())
            .thenComparing(AuthoredNoteReferenceRow::getDocumentOrder));
    return candidates;
  }

  private List<AuthoredNoteReferenceRow> wikiCandidateRowsForTarget(Note target) {
    if (target.getNotebook() == null) {
      return List.of();
    }
    Integer notebookId = target.getNotebook().getId();
    String notebookName = target.getNotebook().getName();
    List<String> aliasLookupKeys = aliasLookupKeysFor(target);
    List<AuthoredNoteReferenceRow> matches = new ArrayList<>();
    for (AuthoredNoteReferenceRow row :
        authoredNoteReferenceRowRepository.findWikiCandidatesForNotebookScope(
            AuthoredNoteReferenceRow.Kind.WIKI_PORTABLE_PATH, notebookName, notebookId)) {
      if (wikiNotePortionMatchesTarget(row.getWikiNotePortion(), target, aliasLookupKeys)) {
        matches.add(row);
      }
    }
    return matches;
  }

  private List<String> aliasLookupKeysFor(Note target) {
    List<String> keys = new ArrayList<>();
    for (var aliasRow : noteAliasIndexRepository.findByNote_IdOrderByIdAsc(target.getId())) {
      keys.add(aliasRow.getAliasLookupKey());
    }
    return keys;
  }

  /**
   * Mirrors the forward wiki-link note-candidate matching concept in reverse: a path-shaped note
   * portion matches by title and folder trail (never alias, same as forward path-shaped matching);
   * a non-path-shaped note portion matches the target's title or one of its current aliases.
   */
  private boolean wikiNotePortionMatchesTarget(
      String notePortion, Note target, List<String> aliasLookupKeys) {
    if (notePortion == null) {
      return false;
    }
    return PathShapedTarget.tryParse(notePortion)
        .map(
            path ->
                path.matchesTitleAndFolderTrail(
                    target.getTitle(), FolderTrailSegments.namesFromRootToContainingFolder(target)))
        .orElseGet(() -> titleOrAliasMatches(notePortion, target, aliasLookupKeys));
  }

  private boolean titleOrAliasMatches(
      String notePortion, Note target, List<String> aliasLookupKeys) {
    if (notePortion.equalsIgnoreCase(target.getTitle())) {
      return true;
    }
    return aliasLookupKeys.contains(FrontmatterAliases.normalizedLookupKey(notePortion));
  }
}
