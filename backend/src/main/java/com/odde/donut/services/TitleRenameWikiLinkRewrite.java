package com.odde.donut.services;

import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.AuthoredNoteReferenceInboundFacade;
import com.odde.donut.entities.repositories.ResolvedWikiLinkRepository;
import com.odde.donut.factoryServices.EntityPersister;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Title-rename wiki-link rewrite for {@link WikiLinkRewriteService}. Discovers referrers whose
 * authored reference live-resolves to {@code targetNote} (ADR 0001 Wiki link) before the rename
 * takes effect, then restricts the inbound rewrite to that live-resolved set: a referrer whose
 * cached inbound row survives but no longer live-resolves is a stale cache row and must not be
 * rewritten.
 */
final class TitleRenameWikiLinkRewrite {

  private TitleRenameWikiLinkRewrite() {}

  static void rewrite(
      EntityManager entityManager,
      ResolvedWikiLinkRepository resolvedWikiLinkRepository,
      EntityPersister entityPersister,
      ResolvedWikiLinkService resolvedWikiLinkService,
      AuthoredNoteReferenceInboundFacade authoredNoteReferenceInboundFacade,
      CanonicalDonutOrigin canonicalDonutOrigin,
      Note targetNote,
      String newTitle,
      Timestamp updatedAt,
      User viewer,
      BiFunction<Note, String, String> linkRewrite) {
    // Discover referrers whose authored reference resolves to targetNote before the rename takes
    // effect: those references are authored against the pre-rename title/aliases, so this must run
    // before the title changes underneath them.
    Set<Integer> liveResolvedReferrerIds =
        authoredNoteReferenceInboundFacade.distinctReferrerIdsForViewer(targetNote, viewer);
    targetNote.setTitle(new DisplayName(newTitle));
    targetNote.setUpdatedAt(updatedAt);
    entityPersister.save(targetNote);
    entityManager.flush();
    WikiLinkRewriteSupport.applyInboundReferrerRewrite(
        entityManager,
        resolvedWikiLinkRepository,
        entityPersister,
        resolvedWikiLinkService,
        canonicalDonutOrigin,
        targetNote,
        updatedAt,
        viewer,
        linkRewrite,
        Set.of(),
        Optional.of(liveResolvedReferrerIds));
  }
}
