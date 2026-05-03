package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.NoteDetailsMarkdown;
import com.odde.doughnut.algorithms.NoteYamlFrontmatterScalars;
import com.odde.doughnut.algorithms.WikiLinkMarkdown;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class WikiLinkResolver {

  private final NoteRepository noteRepository;
  private final AuthorizationService authorizationService;

  public WikiLinkResolver(
      NoteRepository noteRepository, AuthorizationService authorizationService) {
    this.noteRepository = noteRepository;
    this.authorizationService = authorizationService;
  }

  public record ResolvedWikiLink(String linkText, Note targetNote) {}

  public Optional<Note> resolveWikiInnerTitle(
      String wikiLinkInnerTitle, User viewer, Note focusNote) {
    if (wikiLinkInnerTitle == null || wikiLinkInnerTitle.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(resolveToken(wikiLinkInnerTitle.trim(), viewer, focusNote));
  }

  /** Resolves the {@code target:} wiki link from a relationship note's YAML frontmatter. */
  public Optional<Note> resolveSemanticTarget(Note relation, User viewer) {
    String details = relation.getDetails();
    if (details == null || details.isBlank()) {
      return Optional.empty();
    }
    Optional<String> targetScalar =
        NoteDetailsMarkdown.splitLeadingFrontmatter(details)
            .flatMap(fm -> NoteYamlFrontmatterScalars.firstScalarValue(fm.yamlRaw(), "target"));
    if (targetScalar.isEmpty()) {
      return Optional.empty();
    }
    List<String> inners = WikiLinkMarkdown.innerTitlesInOccurrenceOrder(targetScalar.get());
    if (inners.isEmpty()) {
      return Optional.empty();
    }
    return resolveWikiInnerTitle(inners.getFirst(), viewer, relation);
  }

  public List<ResolvedWikiLink> resolveWikiLinksForCache(Note focusNote, User viewer) {
    String details = focusNote.getDetails();
    if (details == null || details.isBlank()) {
      return List.of();
    }
    List<String> linkTitlesOrdered = WikiLinkMarkdown.innerTitlesInOccurrenceOrder(details);
    if (linkTitlesOrdered.isEmpty()) {
      return List.of();
    }
    List<ResolvedWikiLink> out = new ArrayList<>();
    for (String token : dedupePreserveOrder(linkTitlesOrdered)) {
      Note target = resolveToken(token, viewer, focusNote);
      if (target != null) {
        out.add(new ResolvedWikiLink(token, target));
      }
    }
    return List.copyOf(out);
  }

  private Note resolveToken(String token, User viewer, Note focusNote) {
    Qualified qualified = Qualified.tryParse(token);
    if (qualified != null) {
      return firstReadableNotebookMatch(qualified.notebookName(), qualified.noteTitle(), viewer);
    }
    Notebook focusNotebook = focusNote.getNotebook();
    if (focusNotebook == null) {
      return null;
    }
    return firstReadableNotebookMatch(focusNotebook.getName(), token, viewer);
  }

  private Note firstReadableNotebookMatch(String notebookName, String noteTitle, User viewer) {
    List<Note> candidates =
        noteRepository.findByNotebookNameAndNoteTitleOrderByIdAsc(notebookName, noteTitle);
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

  private static List<String> dedupePreserveOrder(List<String> titles) {
    List<String> out = new ArrayList<>();
    Set<String> seenNormalized = new HashSet<>();
    for (String t : titles) {
      String key = Normalizer.normalize(t, Normalizer.Form.NFKC);
      if (seenNormalized.add(key)) {
        out.add(t);
      }
    }
    return out;
  }
}
