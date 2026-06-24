package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.NoteContentMarkdown;
import com.odde.doughnut.algorithms.WikiLinkMarkdown;
import com.odde.doughnut.algorithms.WikiLinkTargetReference;
import com.odde.doughnut.controllers.dto.TitleRenameReferenceHandling;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WikiLinkRewriteService {
  @PersistenceContext private EntityManager entityManager;

  private final NoteWikiTitleCacheRepository noteWikiTitleCacheRepository;
  private final EntityPersister entityPersister;
  private final WikiTitleCacheService wikiTitleCacheService;

  public WikiLinkRewriteService(
      NoteWikiTitleCacheRepository noteWikiTitleCacheRepository,
      EntityPersister entityPersister,
      WikiTitleCacheService wikiTitleCacheService) {
    this.noteWikiTitleCacheRepository = noteWikiTitleCacheRepository;
    this.entityPersister = entityPersister;
    this.wikiTitleCacheService = wikiTitleCacheService;
  }

  /**
   * Rewrites inbound wiki links and rebuilds each changed referrer's wiki-title cache. Persists the
   * renamed note's new title first so updated referrer tokens resolve.
   */
  @Transactional
  public void rewriteInboundWikiLinksForTitleRename(
      Note targetNote,
      String newTitle,
      Timestamp updatedAt,
      User viewer,
      TitleRenameReferenceHandling handling) {
    UnaryOperator<String> fn =
        handling == TitleRenameReferenceHandling.KEEP_VISIBLE_TEXT
            ? lt -> WikiLinkMarkdown.newInnerForKeepVisibleText(lt, newTitle)
            : lt -> WikiLinkMarkdown.newInnerForUpdateVisibleText(lt, newTitle);
    targetNote.setTitle(newTitle);
    targetNote.setUpdatedAt(updatedAt);
    entityPersister.save(targetNote);
    entityManager.flush();
    rewriteInboundWikiLinks(targetNote, updatedAt, viewer, fn, Set.of());
  }

  /**
   * Rewrites inbound wiki links that still use legacy title-alias full titles after title
   * migration. Bare links update visible text to the note's current title; piped links keep
   * explicit display text.
   */
  @Transactional
  public void rewriteInboundWikiLinksForTitleAliasMigration(
      Note targetNote, Timestamp updatedAt, User viewer) {
    rewriteInboundWikiLinks(
        targetNote,
        updatedAt,
        viewer,
        lt -> WikiLinkMarkdown.newInnerForUpdateVisibleText(lt, targetNote.getTitle()),
        Set.of());
  }

  /**
   * Rewrites inbound and outgoing wiki links when a note moves to a different notebook. No-op when
   * the source and target notebooks are the same.
   */
  @Transactional
  public void rewriteWikiLinksForCrossNotebookMove(
      Note movedNote,
      Notebook oldNotebook,
      Notebook targetNotebook,
      Timestamp updatedAt,
      User viewer) {
    Integer oldNotebookId = oldNotebook != null ? oldNotebook.getId() : null;
    if (!Objects.equals(oldNotebookId, targetNotebook.getId())) {
      rewriteInboundWikiLinksForNotebookMove(
          movedNote, targetNotebook.getName(), updatedAt, viewer);
      String oldNotebookName = oldNotebook != null ? oldNotebook.getName() : null;
      rewriteOutgoingWikiLinksForNotebookMove(movedNote, oldNotebookName, updatedAt, viewer);
    }
  }

  /**
   * Rewrites inbound wiki links for a note that has moved to a different notebook. Preserves
   * visible display text while qualifying all tokens with the new notebook name.
   */
  @Transactional
  public void rewriteInboundWikiLinksForNotebookMove(
      Note targetNote, String newNotebookName, Timestamp updatedAt, User viewer) {
    rewriteInboundWikiLinksForNotebookMove(
        targetNote, newNotebookName, updatedAt, viewer, Set.of());
  }

  @Transactional
  public void rewriteInboundWikiLinksForNotebookMove(
      Note targetNote,
      String newNotebookName,
      Timestamp updatedAt,
      User viewer,
      Set<Integer> excludedReferrerIds) {
    UnaryOperator<String> fn =
        lt -> WikiLinkMarkdown.newInnerForKeepNotebookMove(lt, newNotebookName);
    rewriteInboundWikiLinks(targetNote, updatedAt, viewer, fn, excludedReferrerIds);
  }

  /**
   * Rewrites inbound wiki links for every note in a folder subtree that moved to another notebook.
   * Referrers inside the moved set are skipped because their relative links still resolve.
   */
  @Transactional
  public void rewriteInboundWikiLinksForFolderNotebookMove(
      Set<Integer> movedNoteIds, String newNotebookName, Timestamp updatedAt, User viewer) {
    forEachNonDeletedNoteInMoveSet(
        movedNoteIds,
        note ->
            rewriteInboundWikiLinksForNotebookMove(
                note, newNotebookName, updatedAt, viewer, movedNoteIds));
  }

  /**
   * Rewrites outgoing wiki links for every note in a folder subtree that moved to another notebook.
   * Unqualified links to co-moved targets stay relative; links to notes that stayed behind qualify
   * to the source notebook.
   */
  @Transactional
  public void rewriteOutgoingWikiLinksForFolderNotebookMove(
      Set<Integer> movedNoteIds, String sourceNotebookName, Timestamp updatedAt, User viewer) {
    forEachNonDeletedNoteInMoveSet(
        movedNoteIds,
        note ->
            rewriteOutgoingWikiLinksForNotebookMove(
                note, sourceNotebookName, updatedAt, viewer, movedNoteIds));
  }

  /**
   * Rewrites a moved note's own unqualified outgoing wiki links so they keep pointing at its source
   * notebook after the note moves to another notebook.
   */
  @Transactional
  public void rewriteOutgoingWikiLinksForNotebookMove(
      Note movedNote, String sourceNotebookName, Timestamp updatedAt, User viewer) {
    rewriteOutgoingWikiLinksForNotebookMove(
        movedNote, sourceNotebookName, updatedAt, viewer, Set.of());
  }

  @Transactional
  public void rewriteOutgoingWikiLinksForNotebookMove(
      Note movedNote,
      String sourceNotebookName,
      Timestamp updatedAt,
      User viewer,
      Set<Integer> coMovedTargetNoteIds) {
    String originalContent = movedNote.getContent();
    if (originalContent == null || originalContent.isEmpty()) {
      return;
    }
    String content = originalContent;
    LinkedHashSet<String> linkTexts =
        new LinkedHashSet<>(NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder(content));
    for (String linkText : linkTexts) {
      String newInner =
          WikiLinkMarkdown.newInnerForQualifyUnqualifiedOutgoingLink(linkText, sourceNotebookName);
      if (newInner.equals(linkText)) {
        continue;
      }
      if (coMovedTargetResolvesFrom(movedNote, linkText, coMovedTargetNoteIds)) {
        continue;
      }
      content = WikiLinkMarkdown.replaceWikiLinksMatchingTrimmedInner(content, linkText, newInner);
    }
    if (content.equals(originalContent)) {
      return;
    }
    movedNote.setContent(content);
    movedNote.setUpdatedAt(updatedAt);
    entityPersister.save(movedNote);
    wikiTitleCacheService.refreshForNote(movedNote, viewer);
  }

  private void forEachNonDeletedNoteInMoveSet(Set<Integer> movedNoteIds, Consumer<Note> action) {
    if (movedNoteIds.isEmpty()) {
      return;
    }
    List<Integer> noteIds = new ArrayList<>(movedNoteIds);
    Collections.sort(noteIds);
    for (Integer noteId : noteIds) {
      Note note = entityManager.find(Note.class, noteId);
      if (note != null && note.getDeletedAt() == null) {
        action.accept(note);
      }
    }
  }

  private boolean coMovedTargetResolvesFrom(
      Note movedNote, String linkText, Set<Integer> coMovedTargetNoteIds) {
    if (coMovedTargetNoteIds.isEmpty()) {
      return false;
    }
    String focusNotebookName =
        movedNote.getNotebook() == null ? null : movedNote.getNotebook().getName();
    Optional<WikiLinkTargetReference> reference =
        WikiLinkTargetReference.forToken(linkText, focusNotebookName);
    if (reference.isEmpty()) {
      return false;
    }
    WikiLinkTargetReference ref = reference.get();
    List<Integer> noteIds = new ArrayList<>(coMovedTargetNoteIds);
    Collections.sort(noteIds);
    // When several co-moved notes share a title, lowest note id wins (same as global resolution).
    for (Integer noteId : noteIds) {
      Note candidate = entityManager.find(Note.class, noteId);
      if (candidate != null
          && candidate.getDeletedAt() == null
          && noteMatchesWikiLinkTarget(candidate, ref)) {
        return true;
      }
    }
    return false;
  }

  private static boolean noteMatchesWikiLinkTarget(Note note, WikiLinkTargetReference ref) {
    if (note.getNotebook() == null) {
      return false;
    }
    return note.getNotebook().getName().equalsIgnoreCase(ref.notebookName())
        && note.getTitle().equalsIgnoreCase(ref.noteTitle());
  }

  private void rewriteInboundWikiLinks(
      Note targetNote,
      Timestamp updatedAt,
      User viewer,
      UnaryOperator<String> newInnerFromLinkText,
      Set<Integer> excludedReferrerIds) {
    Integer targetId = targetNote.getId();
    List<NoteWikiTitleCache> rows =
        noteWikiTitleCacheRepository.findRowsReferringToNonDeletedNotesForTarget(targetId);

    Map<Integer, LinkedHashSet<String>> linkTextsByReferrer = new LinkedHashMap<>();
    for (NoteWikiTitleCache row : rows) {
      linkTextsByReferrer
          .computeIfAbsent(row.getNote().getId(), _ -> new LinkedHashSet<>())
          .add(row.getLinkText());
    }
    List<Integer> referrerIds = new ArrayList<>(linkTextsByReferrer.keySet());
    Collections.sort(referrerIds);
    for (Integer referrerId : referrerIds) {
      if (excludedReferrerIds.contains(referrerId)) {
        continue;
      }
      Note referrer = entityManager.find(Note.class, referrerId);
      if (referrer == null || referrer.getDeletedAt() != null) {
        continue;
      }
      String content = referrer.getContent() != null ? referrer.getContent() : "";
      for (String linkText : linkTextsByReferrer.get(referrerId)) {
        String newInner = newInnerFromLinkText.apply(linkText);
        content =
            WikiLinkMarkdown.replaceWikiLinksMatchingTrimmedInner(content, linkText, newInner);
      }
      referrer.setContent(content);
      referrer.setUpdatedAt(updatedAt);
      entityPersister.save(referrer);
      wikiTitleCacheService.refreshForNote(referrer, viewer);
    }
  }
}
