package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class WikiLinkResolver {

  private static final Pattern WIKI_LINK = Pattern.compile("\\[\\[([^\\]]+)]]");

  private final NoteRepository noteRepository;
  private final AuthorizationService authorizationService;

  public WikiLinkResolver(
      NoteRepository noteRepository, AuthorizationService authorizationService) {
    this.noteRepository = noteRepository;
    this.authorizationService = authorizationService;
  }

  public record ResolvedWikiLink(String linkText, Note targetNote) {}

  public List<ResolvedWikiLink> resolveWikiLinksForCache(Note focusNote, User viewer) {
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
    List<ResolvedWikiLink> out = new ArrayList<>();
    for (String token : dedupePreserveOrder(linkTitlesOrdered)) {
      Note target = resolveToken(token, noteByExactTitle, viewer, focusNote);
      if (target != null) {
        out.add(new ResolvedWikiLink(token, target));
      }
    }
    return List.copyOf(out);
  }

  private Note resolveToken(
      String token, Map<String, Note> noteByExactTitle, User viewer, Note focusNote) {
    Qualified qualified = Qualified.tryParse(token);
    if (qualified != null) {
      return firstReadableCandidate(qualified, viewer);
    }
    Note target = noteByExactTitle.get(token);
    if (target != null) {
      Notebook notebook = target.getNotebook();
      if (notebook == null) {
        notebook = focusNote.getNotebook();
      }
      if (authorizationService.userMayReadNotebook(viewer, notebook)) {
        return target;
      }
    }
    return null;
  }

  private Note firstReadableCandidate(Qualified qualified, User viewer) {
    List<Note> candidates =
        noteRepository.findByNotebookNameAndNoteTitleOrderByIdAsc(
            qualified.notebookName(), qualified.noteTitle());
    for (Note candidate : candidates) {
      Notebook notebook = candidate.getNotebook();
      if (notebook != null && authorizationService.userMayReadNotebook(viewer, notebook)) {
        return candidate;
      }
    }
    return null;
  }

  private record Qualified(String notebookName, String noteTitle) {
    static Qualified tryParse(String token) {
      int i = token.indexOf(':');
      if (i <= 0 || i >= token.length() - 1) {
        return null;
      }
      String nb = token.substring(0, i).trim();
      String nt = token.substring(i + 1).trim();
      if (nb.isEmpty() || nt.isEmpty()) {
        return null;
      }
      return new Qualified(nb, nt);
    }
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
    mergeNoteByExactTitle(byTitle, root);
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
