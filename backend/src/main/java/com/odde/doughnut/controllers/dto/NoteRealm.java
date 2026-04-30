package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;

@JsonPropertyOrder({
  "id",
  "slug",
  "note",
  "fromBazaar",
  "circle",
  "children",
  "inboundReferences",
  "wikiTitles",
  "notebook"
})
public class NoteRealm {
  private static final Pattern WIKI_LINK = Pattern.compile("\\[\\[([^\\]]+)]]");

  @Getter @Setter private List<Note> inboundReferences;

  @NotNull @Getter private Note note;

  @Getter @Setter private Boolean fromBazaar;

  @Getter private final List<WikiTitle> wikiTitles;

  public NoteRealm(Note note) {
    this.note = note;
    this.wikiTitles = resolveWikiTitles(note);
  }

  @NotNull
  public Integer getId() {
    return note.getId();
  }

  @NonNull
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  public String getSlug() {
    return Objects.requireNonNullElse(note.getSlug(), "");
  }

  public List<Note> getChildren() {
    return note.getChildren();
  }

  @NonNull
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  public Notebook getNotebook() {
    return Objects.requireNonNull(note.getNotebook());
  }

  private static List<WikiTitle> resolveWikiTitles(Note focusNote) {
    String details = focusNote.getDetails();
    if (details == null || details.isBlank()) {
      return List.of();
    }
    List<String> linkTitlesOrdered = extractWikiTitles(details);
    if (linkTitlesOrdered.isEmpty()) {
      return List.of();
    }
    Map<String, Note> noteByExactTitle =
        descendantNotesIndexedByExactTitle(withoutRelations(rootNote(focusNote)));
    List<WikiTitle> out = new ArrayList<>();
    for (String title : dedupePreserveOrder(linkTitlesOrdered)) {
      Note target = noteByExactTitle.get(title);
      if (target != null) {
        out.add(new WikiTitle(title, target.getId()));
      }
    }
    return List.copyOf(out);
  }

  private static Note rootNote(Note note) {
    Note current = note;
    while (current.getParent() != null) {
      current = current.getParent();
    }
    return current;
  }

  private static Note withoutRelations(Note root) {
    return root.isRelation() ? Objects.requireNonNullElse(root.getParent(), root) : root;
  }

  private static List<String> extractWikiTitles(String markdown) {
    Matcher matcher = WIKI_LINK.matcher(markdown);
    List<String> titles = new ArrayList<>();
    while (matcher.find()) {
      String t = matcher.group(1).trim();
      if (!t.isEmpty()) {
        titles.add(t);
      }
    }
    return titles;
  }

  private static Map<String, Note> descendantNotesIndexedByExactTitle(Note root) {
    Map<String, Note> byTitle = new LinkedHashMap<>();
    root.getAllDescendants().forEach(desc -> mergeNoteByExactTitle(byTitle, desc));
    return byTitle;
  }

  /** One note per exact title: smallest id wins (stable, matches first in tree when ids grow). */
  private static void mergeNoteByExactTitle(Map<String, Note> byTitle, Note candidate) {
    if (candidate.isRelation()) {
      return;
    }
    String key = candidate.getTitle() != null ? candidate.getTitle() : "";
    byTitle.merge(key, candidate, (a, b) -> a.getId() <= b.getId() ? a : b);
  }

  private static List<String> dedupePreserveOrder(List<String> titles) {
    List<String> out = new ArrayList<>();
    for (String t : titles) {
      if (!out.contains(t)) {
        out.add(t);
      }
    }
    return out;
  }
}
