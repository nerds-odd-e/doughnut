package com.odde.donut.entities.repositories;

import com.odde.donut.algorithms.AuthoredNoteReference;
import com.odde.donut.algorithms.FrontmatterAliases;
import com.odde.donut.algorithms.NoteReferenceResolution;
import com.odde.donut.entities.AuthoredNoteReferenceRow;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.services.WikiLinkResolver;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
 * <p>Application consumers use {@link com.odde.donut.services.NoteReferenceService} rather than
 * injecting this type. Exposes only domain-shaped results ({@link Note}) to callers, never the
 * internal {@link AuthoredNoteReferenceRow} persistence rows — see {@link
 * AuthoredNoteReferenceRowRepository}.
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
   * viewer}, ordered by referrer note id ascending.
   */
  public List<Note> distinctReferrerNotesForViewer(Note target, User viewer) {
    List<Note> referrers = new ArrayList<>();
    for (InboundReference inboundReference : distinctInboundReferencesForViewer(target, viewer)) {
      referrers.add(inboundReference.referrer());
    }
    return referrers;
  }

  /** Whether any referrer has an authored reference that live-resolves to {@code target}. */
  public boolean isReferencedForViewer(Note target, User viewer) {
    for (AuthoredNoteReferenceRow candidate : candidateRowsForTarget(target)) {
      Note sourceNote = candidate.getNote();
      if (isInboundReference(candidate, sourceNote, target, viewer)) {
        return true;
      }
    }
    return false;
  }

  /**
   * One referrer note plus the distinct authored link text(s) (in document order) it uses to refer
   * to the queried target. A referrer can carry more than one text when it authors several distinct
   * references (wiki and/or note-ID URL) that all resolve to the same target.
   */
  public record InboundReference(Note referrer, List<String> authoredLinkTexts) {}

  /**
   * {@link #distinctReferrerNotesForViewer}, generalized to also carry each surviving candidate
   * row's authored link text — the data a rewrite consumer ({@code WikiLinkRewriteService}) needs
   * to locate and rewrite each referrer's inbound reference(s) to {@code target}, without a
   * separate lookup into cached rows. Ordered by referrer note id ascending.
   */
  public List<InboundReference> distinctInboundReferencesForViewer(Note target, User viewer) {
    LinkedHashMap<Integer, Note> referrersInOrder = new LinkedHashMap<>();
    Map<Integer, LinkedHashSet<String>> linkTextsByReferrerId = new LinkedHashMap<>();
    for (AuthoredNoteReferenceRow candidate : candidateRowsForTarget(target)) {
      Note sourceNote = candidate.getNote();
      Integer sourceNoteId = sourceNote.getId();
      if (!isInboundReference(candidate, sourceNote, target, viewer)) {
        continue;
      }
      referrersInOrder.putIfAbsent(sourceNoteId, sourceNote);
      linkTextsByReferrerId
          .computeIfAbsent(sourceNoteId, _ -> new LinkedHashSet<>())
          .add(candidate.toDomainReference().authoredLink());
    }
    List<InboundReference> results = new ArrayList<>();
    for (var entry : referrersInOrder.entrySet()) {
      results.add(
          new InboundReference(
              entry.getValue(), List.copyOf(linkTextsByReferrerId.get(entry.getKey()))));
    }
    return results;
  }

  private boolean isInboundReference(
      AuthoredNoteReferenceRow candidate, Note sourceNote, Note target, User viewer) {
    return resolvesToTarget(candidate, sourceNote, target, viewer)
        && referrerVisibleToViewer(sourceNote, target, viewer);
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
    String lowerCaseTitle = target.getTitle().toLowerCase(Locale.ROOT);
    List<String> titleAndAliasLookupKeys = new ArrayList<>();
    titleAndAliasLookupKeys.add(FrontmatterAliases.normalizedLookupKey(target.getTitle()));
    for (var aliasRow : noteAliasIndexRepository.findByNote_IdOrderByIdAsc(target.getId())) {
      titleAndAliasLookupKeys.add(aliasRow.getAliasLookupKey());
    }
    return authoredNoteReferenceRowRepository.findWikiCandidatesForNotebookScope(
        AuthoredNoteReferenceRow.Kind.WIKI_PORTABLE_PATH,
        notebookName,
        notebookId,
        titleAndAliasLookupKeys,
        "%/" + lowerCaseTitle,
        "%/" + lowerCaseTitle + ".md");
  }
}
