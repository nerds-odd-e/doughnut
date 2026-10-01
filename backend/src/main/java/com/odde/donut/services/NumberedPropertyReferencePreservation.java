package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteReference;
import com.odde.donut.algorithms.AuthoredNoteReferences;
import com.odde.donut.algorithms.NoteContentMarkdown.ConsolidatedProperties;
import com.odde.donut.algorithms.PropertyKeyNaming;
import com.odde.donut.algorithms.WikiLinkMarkdown;
import com.odde.donut.algorithms.WikiLinkMarkdownDocumentRewrite;
import com.odde.donut.algorithms.WikiLinkMarkdownRewrite;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.UserRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Preserves each current reader's authored selector resolution before any migration mutation. */
@Service
class NumberedPropertyReferencePreservation {
  private final WikiLinkResolver resolver;
  private final AuthorizationService authorization;
  private final UserRepository users;

  NumberedPropertyReferencePreservation(
      WikiLinkResolver resolver, AuthorizationService authorization, UserRepository users) {
    this.resolver = resolver;
    this.authorization = authorization;
    this.users = users;
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

  Rewrites prepare(Note source, Map<Integer, ConsolidatedProperties> projected) {
    List<User> readers = new ArrayList<>();
    users
        .findAll()
        .forEach(
            user -> {
              if (authorization.userMayReadNotebook(user, source.getNotebook())) readers.add(user);
            });
    if (authorization.userMayReadNotebook(null, source.getNotebook())) readers.add(null);
    Map<Integer, String> projectedContent = new LinkedHashMap<>();
    projected.forEach((id, transformed) -> projectedContent.put(id, transformed.content()));
    Map<String, String> rewrites = new LinkedHashMap<>();
    Set<String> seen = new HashSet<>();
    for (var reference :
        AuthoredNoteReferences.inOccurrenceOrder(
            source.getContent(), resolver.canonicalDonutOrigin())) {
      if (!(reference instanceof AuthoredNoteReference.WikiPortablePathTarget wiki)) continue;
      String original = wiki.authoredLink();
      if (!seen.add(original)) continue;
      String rewritten = original;
      String requiredKey = null;
      for (User reader : readers) {
        var before = resolver.classifyToken(original, source, reader);
        if (before instanceof WikiLinkResolver.CandidateCardinality.Resolved resolved) {
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
      for (User reader : readers) {
        var before = resolver.classifyToken(original, source, reader);
        var after = resolver.classifyProjectedToken(rewritten, source, reader, projectedContent);
        if (!sameMeaning(before, after, original, rewritten, projected)) return refused(original);
      }
      if (!original.equals(rewritten)) rewrites.put(original, rewritten);
    }
    return new Rewrites(Map.copyOf(rewrites), null);
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
