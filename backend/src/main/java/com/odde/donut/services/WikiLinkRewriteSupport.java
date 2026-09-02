package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.AuthoredNoteReference;
import com.odde.donut.algorithms.AuthoredNoteReferences;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.algorithms.NoteIdUrl;
import com.odde.donut.algorithms.NoteReferenceResolution;
import com.odde.donut.algorithms.PathShapedTarget;
import com.odde.donut.algorithms.PortablePath;
import com.odde.donut.algorithms.WikiLinkMarkdown;
import com.odde.donut.algorithms.WikiLinkMarkdownDocumentRewrite;
import com.odde.donut.algorithms.WikiLinkMarkdownRewrite;
import com.odde.donut.controllers.dto.FolderTrailSegments;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
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

  /**
   * Authored portable path for {@code targetNote} as referred to by {@code referrer}, preserving
   * the original visible text only when {@code keepVisibleText}. Shared by title rename and
   * notebook move, the two rewrite paths where a referrer's authored path to a note can change.
   */
  static String rewrittenReference(
      PortablePathAuthoring portablePathAuthoring,
      Note referrer,
      Note targetNote,
      String linkText,
      boolean keepVisibleText) {
    String originalPortablePath = WikiLinkMarkdown.splitInner(linkText).portablePath().format();
    String authoredPortablePath =
        portablePathAuthoring.authoredPortablePath(referrer, targetNote, originalPortablePath);
    return WikiLinkMarkdownRewrite.newInnerForAuthoredPortablePath(
        linkText, authoredPortablePath, keepVisibleText);
  }

  /**
   * Rewrites each live, non-excluded referrer's inbound wiki link(s) to {@code targetNote}. {@code
   * authoredLinkTextsByReferrerId} is the candidate set itself — every referrer id it carries has
   * already been confirmed to live-resolve to {@code targetNote} (per {@link
   * com.odde.donut.services.NoteReferenceService}), captured by the caller before {@code
   * targetNote} (or its containing folder/notebook) was relocated, since resolution is authored
   * against the pre-move identity.
   */
  static void applyInboundReferrerRewrite(
      EntityManager entityManager,
      EntityPersister entityPersister,
      NoteReferenceService noteReferenceService,
      CanonicalDonutOrigin canonicalDonutOrigin,
      Note targetNote,
      Timestamp updatedAt,
      BiFunction<Note, String, String> linkRewrite,
      Set<Integer> excludedReferrerIds,
      Map<Integer, List<String>> authoredLinkTextsByReferrerId) {
    List<Integer> referrerIds = new ArrayList<>(authoredLinkTextsByReferrerId.keySet());
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
      for (String linkText : authoredLinkTextsByReferrerId.get(referrerId)) {
        if (NoteIdUrl.isAuthoredMarkdownNoteIdUrl(linkText, canonicalDonutOrigin)) {
          continue;
        }
        String newInner = linkRewrite.apply(referrer, linkText);
        content =
            WikiLinkMarkdownDocumentRewrite.replaceWikiLinksMatchingTrimmedInner(
                content, linkText, newInner);
      }
      referrer.replaceContent(documentFromRewrittenContent(content, canonicalDonutOrigin));
      referrer.setUpdatedAt(updatedAt);
      entityPersister.save(referrer);
      noteReferenceService.refreshDerivedIndexesForNote(referrer);
    }
  }

  static void applyOutgoingNotebookMoveRewrite(
      EntityPersister entityPersister,
      NoteReferenceService noteReferenceService,
      PortablePathAuthoring portablePathAuthoring,
      WikiLinkResolver wikiLinkResolver,
      CanonicalDonutOrigin canonicalDonutOrigin,
      Note movedNote,
      String sourceNotebookName,
      Timestamp updatedAt,
      User viewer,
      Map<String, Note> coMovedTargetsByAuthoredLink) {
    String originalContent = movedNote.getContent();
    if (originalContent == null || originalContent.isEmpty()) {
      return;
    }
    String content = originalContent;
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
    movedNote.replaceContent(documentFromRewrittenContent(content, canonicalDonutOrigin));
    movedNote.setUpdatedAt(updatedAt);
    entityPersister.save(movedNote);
    noteReferenceService.refreshDerivedIndexesForNote(movedNote);
  }

  /**
   * Builds the {@link AuthoredNoteDocument} for content already valid and stored, whose wiki links
   * were mechanically rewritten in place (title/location change). Unlike {@code
   * AuthoredNoteContent#prepareDocumentForSave}, this does not re-validate or re-normalize the
   * stored type — a rewrite must not change the established visible content beyond the link text
   * itself.
   */
  private static AuthoredNoteDocument documentFromRewrittenContent(
      String content, CanonicalDonutOrigin canonicalDonutOrigin) {
    List<AuthoredNoteReference> references =
        AuthoredNoteReferences.uniquePreserveOrder(
            AuthoredNoteReferences.inOccurrenceOrder(content, canonicalDonutOrigin));
    return new AuthoredNoteDocument(content, references);
  }

  /**
   * {@code sourceNote}'s own outgoing wiki Portable-path targets, live-resolved against {@code
   * sourceNote}'s current notebook scope, restricted to references that resolve to another note
   * within {@code candidateTargetNoteIds}. Must be captured before {@code sourceNote} (or the notes
   * it may target) is relocated: resolution is scoped to the notebook as it stood at capture time,
   * mirroring inbound capture ({@link com.odde.donut.services.NoteReferenceService}).
   */
  static Map<String, Note> liveResolvedOutgoingWikiLinksToNotes(
      WikiLinkResolver wikiLinkResolver,
      Note sourceNote,
      User viewer,
      Set<Integer> candidateTargetNoteIds) {
    String content = sourceNote.getContent();
    if (content == null || content.isEmpty()) {
      return Map.of();
    }
    Map<String, Note> byLinkText = new LinkedHashMap<>();
    for (var wiki : AuthoredNoteReferences.uniqueWikiPortablePathTargets(content)) {
      NoteReferenceResolution resolution =
          wikiLinkResolver.resolveReference(wiki, sourceNote, viewer);
      if (resolution instanceof NoteReferenceResolution.Resolved resolved
          && candidateTargetNoteIds.contains(resolved.destinationNote().getId())) {
        byLinkText.put(wiki.authoredLink(), resolved.destinationNote());
      }
    }
    return byLinkText;
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
