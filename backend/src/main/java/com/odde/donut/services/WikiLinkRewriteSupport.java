package com.odde.donut.services;

import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.algorithms.PathShapedTarget;
import com.odde.donut.algorithms.PortablePath;
import com.odde.donut.algorithms.WikiLinkMarkdown;
import com.odde.donut.algorithms.WikiLinkMarkdownRewrite;
import com.odde.donut.controllers.dto.FolderTrailSegments;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.ResolvedWikiLink;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.ResolvedWikiLinkRepository;
import com.odde.donut.factoryServices.EntityPersister;
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
      ResolvedWikiLinkRepository resolvedWikiLinkRepository,
      EntityPersister entityPersister,
      ResolvedWikiLinkService resolvedWikiLinkService,
      Note targetNote,
      Timestamp updatedAt,
      User viewer,
      UnaryOperator<String> newInnerFromLinkText,
      Set<Integer> excludedReferrerIds) {
    Integer targetId = targetNote.getId();
    List<ResolvedWikiLink> rows =
        resolvedWikiLinkRepository.findRowsReferringToNonDeletedNotesForTarget(targetId);

    Map<Integer, LinkedHashSet<String>> linkTextsByReferrer = new LinkedHashMap<>();
    for (ResolvedWikiLink row : rows) {
      linkTextsByReferrer
          .computeIfAbsent(row.getSourceNote().getId(), _ -> new LinkedHashSet<>())
          .add(row.getAuthoredLink());
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
      resolvedWikiLinkService.refreshForNote(referrer, viewer);
    }
  }

  static void applyOutgoingNotebookMoveRewrite(
      EntityManager entityManager,
      EntityPersister entityPersister,
      ResolvedWikiLinkService resolvedWikiLinkService,
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
    resolvedWikiLinkService.refreshForNote(movedNote, viewer);
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
    Optional<PortablePath.Resolved> reference =
        WikiLinkMarkdown.splitAuthoredToken(linkText).portablePath().resolve(focusNotebookName);
    if (reference.isEmpty()) {
      return false;
    }
    PortablePath.Resolved ref = reference.get();
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

  private static boolean noteMatchesWikiLinkTarget(Note note, PortablePath.Resolved ref) {
    if (note.getNotebook() == null) {
      return false;
    }
    if (!note.getNotebook().getName().equalsIgnoreCase(ref.notebookName())) {
      return false;
    }
    return PathShapedTarget.tryParse(ref.noteTitle())
        .map(
            path ->
                path.matchesTitleAndFolderTrail(
                    note.getTitle(), FolderTrailSegments.namesFromRootToContainingFolder(note)))
        .orElseGet(() -> note.getTitle().equalsIgnoreCase(ref.noteTitle()));
  }
}
