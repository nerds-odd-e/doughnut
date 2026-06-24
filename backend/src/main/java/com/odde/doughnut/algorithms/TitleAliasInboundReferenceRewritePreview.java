package com.odde.doughnut.algorithms;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Pure preview for inbound wiki links that still target legacy title-alias full titles. */
public final class TitleAliasInboundReferenceRewritePreview {

  private TitleAliasInboundReferenceRewritePreview() {}

  public record Item(
      int referrerNoteId,
      int targetNoteId,
      String currentLinkInner,
      String plannedLinkInner,
      boolean visibleTextWillChange) {}

  public static List<Integer> targetNoteIdsWithPendingRewrites(Iterable<NoteWikiTitleCache> rows) {
    Set<Integer> ids = new LinkedHashSet<>();
    for (NoteWikiTitleCache row : rows) {
      previewRow(row).ifPresent(item -> ids.add(item.targetNoteId()));
    }
    return List.copyOf(ids);
  }

  public static List<Item> previewRows(Iterable<NoteWikiTitleCache> rows) {
    List<Item> previews = new ArrayList<>();
    for (NoteWikiTitleCache row : rows) {
      previewRow(row).ifPresent(previews::add);
    }
    return List.copyOf(previews);
  }

  public static Optional<Item> previewRow(NoteWikiTitleCache row) {
    if (row == null || row.getNote() == null || row.getTargetNote() == null) {
      return Optional.empty();
    }
    Note referrer = row.getNote();
    Note target = row.getTargetNote();
    if (referrer.getDeletedAt() != null || target.getDeletedAt() != null) {
      return Optional.empty();
    }
    String linkText = row.getLinkText();
    if (linkText == null || linkText.isBlank()) {
      return Optional.empty();
    }
    String legacyNoteTitle =
        noteTitleTokenFromLinkTarget(WikiLinkMarkdown.splitInner(linkText).target());
    if (!TitleAliasMigrationPlan.from(legacyNoteTitle).hasMigratablePlainAliases()) {
      return Optional.empty();
    }
    String plannedInner =
        WikiLinkMarkdown.newInnerForUpdateVisibleText(linkText, target.getTitle());
    if (plannedInner.equals(linkText.trim())) {
      return Optional.empty();
    }
    WikiLinkMarkdown.WikiInnerSplit split = WikiLinkMarkdown.splitInner(linkText);
    boolean visibleTextWillChange = split.target().trim().equals(split.display().trim());
    return Optional.of(
        new Item(referrer.getId(), target.getId(), linkText, plannedInner, visibleTextWillChange));
  }

  private static String noteTitleTokenFromLinkTarget(String targetToken) {
    if (targetToken == null) {
      return "";
    }
    String trimmed = targetToken.trim();
    int colon = trimmed.indexOf(':');
    if (colon <= 0 || colon >= trimmed.length() - 1) {
      return trimmed;
    }
    return trimmed.substring(colon + 1).trim();
  }
}
