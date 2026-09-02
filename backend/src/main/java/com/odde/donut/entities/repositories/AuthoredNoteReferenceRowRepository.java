package com.odde.donut.entities.repositories;

import com.odde.donut.entities.AuthoredNoteReferenceRow;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Internal to the note-reference persistence boundary: {@link AuthoredNoteReferenceRow} is a
 * persistence row, never exposed to consumers outside this package. Domain references and
 * resolutions are exposed instead (see {@code com.odde.donut.algorithms.AuthoredNoteReference} and
 * {@link AuthoredNoteReferenceInboundFacade}). Kept package-private so it cannot be injected or
 * queried from outside by mistake.
 */
interface AuthoredNoteReferenceRowRepository
    extends JpaRepository<AuthoredNoteReferenceRow, Integer> {

  List<AuthoredNoteReferenceRow> findByNote_IdOrderByDocumentOrderAsc(Integer noteId);

  /**
   * Note-ID-URL candidate rows authored against {@code targetNoteId}, regardless of whether they
   * currently resolve to it (the deleted/unreadable/retargeted target still authors a candidate
   * row) — resolution is verified again by {@link
   * com.odde.donut.services.WikiLinkResolver#resolveReference}. Ordered by referrer note id then
   * document order, the same deterministic order inbound note-reference queries guarantee.
   */
  @Query(
      "SELECT r FROM AuthoredNoteReferenceRow r JOIN FETCH r.note n "
          + "WHERE r.kind = :kind AND r.noteIdUrlNoteId = :targetNoteId "
          + "ORDER BY n.id ASC, r.documentOrder ASC")
  List<AuthoredNoteReferenceRow> findNoteIdUrlCandidatesForTarget(
      @Param("kind") AuthoredNoteReferenceRow.Kind kind,
      @Param("targetNoteId") Integer targetNoteId);

  /**
   * Wiki candidate rows notebook-scoped to a target's current notebook identity: either explicitly
   * qualified with {@code notebookName}, or unqualified and authored from a source note within
   * {@code notebookId} (the source-notebook fallback unqualified wiki links resolve against). Note-
   * portion matching is narrowed to the target's current title/alias lookup keys, plus
   * Portable-path spellings ending in its current title. Candidate rows are always re-verified by
   * the domain resolver, never treated as a resolution verdict.
   */
  @Query(
      "SELECT r FROM AuthoredNoteReferenceRow r JOIN FETCH r.note n "
          + "WHERE r.kind = :kind "
          + "AND ((r.wikiNotebookQualifier IS NOT NULL AND r.wikiNotebookQualifier = :notebookName) "
          + "OR (r.wikiNotebookQualifier IS NULL AND n.notebook.id = :notebookId)) "
          + "AND (LOWER(r.wikiNotePortion) IN :titleAndAliasLookupKeys "
          + "OR LOWER(r.wikiNotePortion) LIKE :pathTitleSuffix "
          + "OR LOWER(r.wikiNotePortion) LIKE :pathMarkdownTitleSuffix) "
          + "ORDER BY n.id ASC, r.documentOrder ASC")
  List<AuthoredNoteReferenceRow> findWikiCandidatesForNotebookScope(
      @Param("kind") AuthoredNoteReferenceRow.Kind kind,
      @Param("notebookName") String notebookName,
      @Param("notebookId") Integer notebookId,
      @Param("titleAndAliasLookupKeys") List<String> titleAndAliasLookupKeys,
      @Param("pathTitleSuffix") String pathTitleSuffix,
      @Param("pathMarkdownTitleSuffix") String pathMarkdownTitleSuffix);
}
