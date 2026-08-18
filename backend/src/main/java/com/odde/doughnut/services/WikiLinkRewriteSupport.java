package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.NoteContentMarkdown;
import com.odde.doughnut.algorithms.WikiLinkMarkdownRewrite;
import com.odde.doughnut.algorithms.WikiLinkTargetReference;
import com.odde.doughnut.controllers.dto.FolderTrailSegments;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/** Persistence helpers for {@link WikiLinkRewriteService}. */
final class WikiLinkRewriteSupport {

  private WikiLinkRewriteSupport() {}

  static void forEachNonDeletedNoteInMoveSet(
      EntityManager entityManager, Set<Integer> movedNoteIds, Consumer<Note> action) {
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

  static void applyInboundReferrerRewrite(
      EntityManager entityManager,
      NoteWikiTitleCacheRepository noteWikiTitleCacheRepository,
      EntityPersister entityPersister,
      WikiTitleCacheService wikiTitleCacheService,
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
            WikiLinkMarkdownRewrite.replaceWikiLinksMatchingTrimmedInner(
                content, linkText, newInner);
      }
      referrer.setContent(content);
      referrer.setUpdatedAt(updatedAt);
      entityPersister.save(referrer);
      wikiTitleCacheService.refreshForNote(referrer, viewer);
    }
  }

  static void applyOutgoingNotebookMoveRewrite(
      EntityManager entityManager,
      EntityPersister entityPersister,
      WikiTitleCacheService wikiTitleCacheService,
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
        new LinkedHashSet<>(NoteContentMarkdown.authoredTokensInOccurrenceOrder(content));
    for (String linkText : linkTexts) {
      String newInner =
          WikiLinkMarkdownRewrite.newInnerForQualifyUnqualifiedOutgoingLink(
              linkText, sourceNotebookName);
      if (newInner.equals(linkText)) {
        continue;
      }
      if (coMovedTargetResolvesFrom(entityManager, movedNote, linkText, coMovedTargetNoteIds)) {
        continue;
      }
      content =
          WikiLinkMarkdownRewrite.replaceWikiLinksMatchingTrimmedInner(content, linkText, newInner);
    }
    if (content.equals(originalContent)) {
      return;
    }
    movedNote.setContent(content);
    movedNote.setUpdatedAt(updatedAt);
    entityPersister.save(movedNote);
    wikiTitleCacheService.refreshForNote(movedNote, viewer);
  }

  private static boolean coMovedTargetResolvesFrom(
      EntityManager entityManager,
      Note movedNote,
      String linkText,
      Set<Integer> coMovedTargetNoteIds) {
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
    if (!note.getNotebook().getName().equalsIgnoreCase(ref.notebookName())) {
      return false;
    }
    return WikiLinkTargetReference.PathShapedTarget.tryParse(ref.noteTitle())
        .map(
            path ->
                path.matchesTitleAndFolderTrail(
                    note.getTitle(), FolderTrailSegments.namesFromRootToContainingFolder(note)))
        .orElseGet(() -> note.getTitle().equalsIgnoreCase(ref.noteTitle()));
  }
}
