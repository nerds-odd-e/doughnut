package com.odde.donut.services;

import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.factoryServices.EntityPersister;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Title-rename wiki-link rewrite for {@link WikiLinkRewriteService}. Rewrites inbound references
 * restricted to the live-resolved set the caller captured before the rename took effect: a referrer
 * whose cached inbound row survives but no longer live-resolves is a stale cache row and must not
 * be rewritten.
 */
final class TitleRenameWikiLinkRewrite {

  private TitleRenameWikiLinkRewrite() {}

  static void rewrite(
      EntityManager entityManager,
      EntityPersister entityPersister,
      ResolvedWikiLinkService resolvedWikiLinkService,
      CanonicalDonutOrigin canonicalDonutOrigin,
      Note targetNote,
      String newTitle,
      Timestamp updatedAt,
      User viewer,
      Map<Integer, List<String>> inboundReferences,
      BiFunction<Note, String, String> linkRewrite) {
    targetNote.setTitle(new DisplayName(newTitle));
    targetNote.setUpdatedAt(updatedAt);
    entityPersister.save(targetNote);
    entityManager.flush();
    WikiLinkRewriteSupport.applyInboundReferrerRewrite(
        entityManager,
        entityPersister,
        resolvedWikiLinkService,
        canonicalDonutOrigin,
        targetNote,
        updatedAt,
        viewer,
        linkRewrite,
        Set.of(),
        inboundReferences);
  }
}
