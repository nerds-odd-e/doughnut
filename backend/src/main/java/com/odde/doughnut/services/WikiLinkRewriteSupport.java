package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.WikiLinkMarkdownRewrite;
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
}
