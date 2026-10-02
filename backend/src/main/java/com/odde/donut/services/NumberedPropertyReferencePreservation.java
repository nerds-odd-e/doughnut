package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteReference;
import com.odde.donut.algorithms.AuthoredNoteReferences;
import com.odde.donut.algorithms.NoteContentMarkdown.ConsolidatedProperties;
import com.odde.donut.algorithms.PropertyKeyNaming;
import com.odde.donut.algorithms.WikiLinkMarkdown;
import com.odde.donut.algorithms.WikiLinkMarkdownDocumentRewrite;
import com.odde.donut.algorithms.WikiLinkMarkdownRewrite;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NotebookRepository;
import com.odde.donut.entities.repositories.UserRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;

/** Preserves each current reader's authored selector resolution before any migration mutation. */
@Service
class NumberedPropertyReferencePreservation {
  private final WikiLinkResolver resolver;
  private final AuthorizationService authorization;
  private final UserRepository users;
  private final NotebookRepository notebooks;

  NumberedPropertyReferencePreservation(
      WikiLinkResolver resolver,
      AuthorizationService authorization,
      UserRepository users,
      NotebookRepository notebooks) {
    this.resolver = resolver;
    this.authorization = authorization;
    this.users = users;
    this.notebooks = notebooks;
  }

  record Rewrites(Map<String, String> inners, String diagnostic) {
    String apply(String content) {
      for (var rewrite : inners.entrySet()) {
        content =
            WikiLinkMarkdownDocumentRewrite.replaceWikiLinksMatchingTrimmedInner(
                content, rewrite.getKey(), rewrite.getValue());
      }
      return content;
    }
  }

  /** One preflight of {@code notebookId}'s projection, reusing reader lookups across its notes. */
  Check check(Integer notebookId, Map<Integer, ConsolidatedProperties> projected) {
    return new Check(notebookId, projected);
  }

  final class Check {
    private final Integer notebookId;
    private final Map<Integer, ConsolidatedProperties> projected;
    private final Map<Integer, String> projectedContent = new LinkedHashMap<>();
    private final Map<String, Boolean> namesProjectedNotebook = new HashMap<>();
    private final Map<Integer, List<User>> readersByNotebook = new HashMap<>();
    private final Map<List<Integer>, Boolean> readable = new HashMap<>();
    private List<User> allUsers;

    private Check(Integer notebookId, Map<Integer, ConsolidatedProperties> projected) {
      this.notebookId = notebookId;
      this.projected = projected;
      projected.forEach((id, transformed) -> projectedContent.put(id, transformed.content()));
    }

    /**
     * Candidates are scoped by notebook name, so only links naming the projected notebook can
     * change resolution; every other note keeps its content and learning unchanged.
     */
    boolean mayAffect(Note source) {
      return projected.containsKey(source.getId())
          || wikiLinks(source).stream()
              .anyMatch(
                  link ->
                      WikiLinkMarkdown.splitInner(link)
                          .portablePath()
                          .resolve(source.getNotebook().getName())
                          .map(
                              ref ->
                                  namesProjectedNotebook.computeIfAbsent(
                                      ref.notebookName(),
                                      name -> notebooks.nameMatches(notebookId, name)))
                          .orElse(false));
    }

    Rewrites prepare(Note source) {
      List<User> readers = readers(source.getNotebook());
      Map<String, String> rewrites = new LinkedHashMap<>();
      Set<String> seen = new HashSet<>();
      for (String original : wikiLinks(source)) {
        if (!seen.add(original)) continue;
        var before = resolver.tokenCandidates(original, source, Map.of());
        String rewritten = original;
        String requiredKey = null;
        for (User reader : readers) {
          if (before.classifyFor(readableBy(reader))
              instanceof WikiLinkResolver.CandidateCardinality.Resolved resolved) {
            String key = mappedKey(original, projected.get(resolved.destinationNote().getId()));
            if (key != null
                && !key.equals(
                    WikiLinkMarkdown.splitInner(original)
                        .portablePath()
                        .decodedPropertyKey()
                        .orElse(null))) {
              if (requiredKey != null && !requiredKey.equals(key)) return refused(original);
              requiredKey = key;
              rewritten = WikiLinkMarkdownRewrite.retargetProperty(original, key);
            }
          }
        }
        var after = resolver.tokenCandidates(rewritten, source, projectedContent);
        for (User reader : readers) {
          if (!sameMeaning(
              before.classifyFor(readableBy(reader)),
              after.classifyFor(readableBy(reader)),
              original,
              rewritten,
              projected)) return refused(original);
        }
        if (!original.equals(rewritten)) rewrites.put(original, rewritten);
      }
      return new Rewrites(Map.copyOf(rewrites), null);
    }

    private List<String> wikiLinks(Note source) {
      List<String> links = new ArrayList<>();
      for (var reference :
          AuthoredNoteReferences.inOccurrenceOrder(
              source.getContent(), resolver.canonicalDonutOrigin())) {
        if (reference instanceof AuthoredNoteReference.WikiPortablePathTarget wiki) {
          links.add(wiki.authoredLink());
        }
      }
      return links;
    }

    private List<User> readers(Notebook notebook) {
      return readersByNotebook.computeIfAbsent(
          notebook.getId(),
          id -> {
            if (allUsers == null) {
              allUsers = new ArrayList<>();
              users.findAll().forEach(allUsers::add);
            }
            List<User> result = new ArrayList<>();
            for (User user : allUsers) {
              if (authorization.userMayReadNotebook(user, notebook)) result.add(user);
            }
            if (authorization.userMayReadNotebook(null, notebook)) result.add(null);
            return result;
          });
    }

    private Predicate<Notebook> readableBy(User reader) {
      return notebook ->
          readable.computeIfAbsent(
              Arrays.asList(reader == null ? null : reader.getId(), notebook.getId()),
              key -> authorization.userMayReadNotebook(reader, notebook));
    }
  }

  private static Rewrites refused(String token) {
    return new Rewrites(Map.of(), "Inconsistent reader resolution: " + token);
  }

  private static String mappedKey(String token, ConsolidatedProperties transformed) {
    if (transformed == null) return null;
    String key =
        WikiLinkMarkdown.splitInner(token).portablePath().decodedPropertyKey().orElse(null);
    if (key == null || !transformed.sourceKeys().contains(key)) return null;
    return PropertyKeyNaming.propertyKeyBaseAndSuffix(key).base();
  }

  private static boolean sameMeaning(
      WikiLinkResolver.CandidateCardinality before,
      WikiLinkResolver.CandidateCardinality after,
      String original,
      String rewritten,
      Map<Integer, ConsolidatedProperties> projected) {
    if (before instanceof WikiLinkResolver.CandidateCardinality.Resolved resolved) {
      if (!(after instanceof WikiLinkResolver.CandidateCardinality.Resolved next)
          || !resolved.destinationNote().getId().equals(next.destinationNote().getId()))
        return false;
      String originalKey =
          WikiLinkMarkdown.splitInner(original).portablePath().decodedPropertyKey().orElse(null);
      String expected = mappedKey(original, projected.get(resolved.destinationNote().getId()));
      if (expected == null) expected = originalKey;
      return Objects.equals(
          expected,
          WikiLinkMarkdown.splitInner(rewritten).portablePath().decodedPropertyKey().orElse(null));
    }
    return before.getClass().equals(after.getClass());
  }
}
