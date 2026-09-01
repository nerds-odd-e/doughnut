package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteReferences;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.algorithms.NoteIdUrl;
import com.odde.donut.algorithms.PathShapedTarget;
import com.odde.donut.algorithms.PortablePath;
import com.odde.donut.algorithms.WikiLinkMarkdown;
import com.odde.donut.algorithms.WikiLinkMarkdownDocumentRewrite;
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
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Wiki Portable-path rewrite helpers for {@link WikiLinkRewriteService}. Note-ID URLs are skipped.
 */
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
      CanonicalDonutOrigin canonicalDonutOrigin,
      Note targetNote,
      Timestamp updatedAt,
      User viewer,
      BiFunction<Note, String, String> linkRewrite,
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
        if (NoteIdUrl.isAuthoredMarkdownNoteIdUrl(linkText, canonicalDonutOrigin)) {
          continue;
        }
        String newInner = linkRewrite.apply(referrer, linkText);
        content =
            WikiLinkMarkdownDocumentRewrite.replaceWikiLinksMatchingTrimmedInner(
                content, linkText, newInner);
      }
      referrer.setContent(content);
      referrer.setUpdatedAt(updatedAt);
      entityPersister.save(referrer);
      resolvedWikiLinkService.refreshForNote(referrer, viewer);
    }
  }

  static void applyOutgoingNotebookMoveRewrite(
      ResolvedWikiLinkRepository resolvedWikiLinkRepository,
      EntityPersister entityPersister,
      ResolvedWikiLinkService resolvedWikiLinkService,
      PortablePathAuthoring portablePathAuthoring,
      WikiLinkResolver wikiLinkResolver,
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
    Map<String, Note> coMovedTargetsByAuthoredLink =
        coMovedTargetsByAuthoredLink(resolvedWikiLinkRepository, movedNote, coMovedTargetNoteIds);
    LinkedHashSet<String> linkTexts = new LinkedHashSet<>();
    for (var wiki : AuthoredNoteReferences.uniqueWikiPortablePathTargets(content)) {
      linkTexts.add(wiki.authoredLink());
    }
    for (String linkText : linkTexts) {
      Note coMovedTarget = coMovedTargetsByAuthoredLink.get(linkText);
      String newInner;
      if (coMovedTarget != null) {
        String originalPortablePath = WikiLinkMarkdown.splitInner(linkText).portablePath().format();
        String authoredPortablePath =
            portablePathAuthoring.authoredPortablePath(
                movedNote, coMovedTarget, originalPortablePath);
        newInner =
            authoredPortablePath.equals(originalPortablePath)
                    || existingPathStillAddresses(movedNote, coMovedTarget, originalPortablePath)
                ? linkText
                : WikiLinkMarkdownRewrite.newInnerForAuthoredPortablePath(
                    linkText, authoredPortablePath, true);
      } else if (wikiLinkResolver.classifyToken(linkText, sourceNotebookName, viewer)
          instanceof WikiLinkResolver.CandidateCardinality.Ambiguous) {
        // Already ambiguous before the move: don't guess which candidate it meant.
        newInner = linkText;
      } else {
        newInner =
            WikiLinkMarkdownRewrite.newInnerForQualifyUnqualifiedOutgoingLink(
                linkText, sourceNotebookName);
      }
      if (newInner.equals(linkText)) {
        continue;
      }
      content =
          WikiLinkMarkdownDocumentRewrite.replaceWikiLinksMatchingTrimmedInner(
              content, linkText, newInner);
    }
    if (content.equals(originalContent)) {
      return;
    }
    movedNote.setContent(content);
    movedNote.setUpdatedAt(updatedAt);
    entityPersister.save(movedNote);
    resolvedWikiLinkService.refreshForNote(movedNote, viewer);
  }

  private static Map<String, Note> coMovedTargetsByAuthoredLink(
      ResolvedWikiLinkRepository resolvedWikiLinkRepository,
      Note movedNote,
      Set<Integer> coMovedTargetNoteIds) {
    Map<String, Note> targets = new LinkedHashMap<>();
    for (ResolvedWikiLink resolvedLink :
        resolvedWikiLinkRepository.findBySourceNote_IdOrderByIdAsc(movedNote.getId())) {
      Note destination = resolvedLink.getDestinationNote();
      if (coMovedTargetNoteIds.contains(destination.getId())) {
        targets.put(resolvedLink.getAuthoredLink(), destination);
      }
    }
    return targets;
  }

  private static boolean existingPathStillAddresses(
      Note sourceNote, Note destinationNote, String portablePath) {
    String sourceNotebookName =
        sourceNote.getNotebook() == null ? null : sourceNote.getNotebook().getName();
    return PortablePath.parse(portablePath)
        .resolve(sourceNotebookName)
        .filter(
            resolved ->
                destinationNote.getNotebook() != null
                    && destinationNote
                        .getNotebook()
                        .getName()
                        .equalsIgnoreCase(resolved.notebookName()))
        .flatMap(
            resolved ->
                PathShapedTarget.tryParse(resolved.noteTitle())
                    .filter(
                        path ->
                            path.matchesTitleAndFolderTrail(
                                destinationNote.getTitle(),
                                FolderTrailSegments.namesFromRootToContainingFolder(
                                    destinationNote))))
        .isPresent();
  }
}
