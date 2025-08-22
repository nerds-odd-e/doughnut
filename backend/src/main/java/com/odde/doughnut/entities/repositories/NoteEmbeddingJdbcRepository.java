package com.odde.doughnut.entities.repositories;

import java.util.Optional;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;

@Repository
public class NoteEmbeddingJdbcRepository {
  private final JdbcTemplate jdbcTemplate;
  private final boolean prodProfile;
  private final String embeddingColumnName; // "embedding" (prod) or "embedding_raw" (others)
  private static final float DEFAULT_MAX_COMBINED_DISTANCE = 1.5f;

  public NoteEmbeddingJdbcRepository(JdbcTemplate jdbcTemplate, Environment environment) {
    this.jdbcTemplate = jdbcTemplate;
    this.prodProfile = environment.acceptsProfiles(Profiles.of("prod"));
    this.embeddingColumnName = this.prodProfile ? "embedding" : "embedding_raw";
  }

  private String embeddingColumn() {
    return embeddingColumnName;
  }

  private boolean isVectorColumn() {
    return prodProfile;
  }

  public void insert(Integer noteId, java.util.List<? extends Number> embeddingFloats) {
    if (isVectorColumn()) {
      // GCP Cloud SQL VECTOR column: use string_to_vector(JSON)
      String json = floatsToJson(embeddingFloats);
      String sql =
          "INSERT INTO note_embeddings (note_id, created_at, updated_at, "
              + embeddingColumn()
              + ") VALUES (?, NOW(), NOW(), string_to_vector(?))";
      jdbcTemplate.update(
          sql,
          (PreparedStatementSetter)
              ps -> {
                ps.setInt(1, noteId);
                ps.setString(2, json);
              });
    } else {
      // Local VARBINARY column
      byte[] bytes = floatsToBytes(embeddingFloats);
      String sql =
          "INSERT INTO note_embeddings (note_id, created_at, updated_at, "
              + embeddingColumn()
              + ") VALUES (?, NOW(), NOW(), ?)";
      jdbcTemplate.update(
          sql,
          (PreparedStatementSetter)
              ps -> {
                ps.setInt(1, noteId);
                ps.setBytes(2, bytes);
              });
    }
  }

  public Optional<byte[]> select(Integer noteId) {
    if (isVectorColumn()) {
      // Not needed for current flows; return empty to avoid relying on vector-to-bytes conversion
      return Optional.empty();
    }
    String sql =
        "SELECT "
            + embeddingColumn()
            + " FROM note_embeddings WHERE note_id=? ORDER BY id DESC LIMIT 1";
    try {
      byte[] bytes = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> rs.getBytes(1), noteId);
      return Optional.ofNullable(bytes);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  public static class SimilarNoteRow {
    public final Integer noteId;
    public final float titleDist;
    public final float detailsDist;
    public final float combinedDist;

    public SimilarNoteRow(Integer noteId, float titleDist, float detailsDist, float combinedDist) {
      this.noteId = noteId;
      this.titleDist = titleDist;
      this.detailsDist = detailsDist;
      this.combinedDist = combinedDist;
    }
  }

  /**
   * Perform semantic KNN search (Cloud SQL vector functions). Returns empty when vector column is
   * unavailable (non-prod), so callers can fallback to literal search.
   */
  public java.util.List<SimilarNoteRow> semanticKnnSearch(
      Integer userId,
      Integer notebookId,
      boolean allMyNotebooksAndSubscriptions,
      boolean allMyCircles,
      java.util.List<? extends Number> queryEmbedding,
      int limit) {
    Scope scope = buildScope(userId, notebookId, allMyNotebooksAndSubscriptions, allMyCircles);
    if (scope == null) {
      return java.util.List.of();
    }

    if (!isVectorColumn()) {
      return new NonProdNoteEmbeddingSemanticSearcher()
          .search(
              jdbcTemplate,
              embeddingColumn(),
              scope.whereClause,
              scope.params,
              queryEmbedding,
              limit,
              DEFAULT_MAX_COMBINED_DISTANCE);
    }

    String embeddingJson = floatsToJson(queryEmbedding);

    java.util.List<Object> params = new java.util.ArrayList<>();
    // First parameter is the embedding JSON for string_to_vector
    params.add(embeddingJson);
    params.addAll(scope.params);

    String sql =
        "WITH q AS (SELECT string_to_vector(?) AS qv) "
            + "SELECT ne.note_id, "
            + " vector_distance("
            + embeddingColumn()
            + ", q.qv, 'distance_measure=l2_squared') AS title_dist, "
            + " 1e9 AS details_dist, "
            + " vector_distance("
            + embeddingColumn()
            + ", q.qv, 'distance_measure=l2_squared') AS combined_dist "
            + "FROM note_embeddings ne "
            + "JOIN q "
            + "JOIN note n ON n.id = ne.note_id AND n.deleted_at IS NULL "
            + "JOIN notebook nb ON nb.id = n.notebook_id AND nb.deleted_at IS NULL "
            + "LEFT JOIN ownership o ON o.id = nb.ownership_id "
            + "WHERE "
            + scope.whereClause
            + " HAVING combined_dist <= ? "
            + " ORDER BY combined_dist ASC "
            + " LIMIT ?";

    params.add(DEFAULT_MAX_COMBINED_DISTANCE);
    params.add(limit);

    return jdbcTemplate.query(
        sql,
        ps -> {
          for (int i = 0; i < params.size(); i++) {
            Object p = params.get(i);
            if (p instanceof String s) {
              ps.setString(i + 1, s);
            } else if (p instanceof Integer ii) {
              ps.setInt(i + 1, ii);
            } else if (p instanceof Long l) {
              ps.setLong(i + 1, l);
            } else if (p instanceof Number n) {
              ps.setDouble(i + 1, n.doubleValue());
            } else {
              ps.setObject(i + 1, p);
            }
          }
        },
        (rs, rowNum) ->
            new SimilarNoteRow(
                rs.getInt("note_id"),
                rs.getFloat("title_dist"),
                rs.getFloat("details_dist"),
                rs.getFloat("combined_dist")));
  }

  /**
   * Return note IDs in a notebook that need index update: either no TITLE embedding exists yet, or
   * the note.updated_at is newer than the latest TITLE embedding updated_at.
   */
  public java.util.List<Integer> selectNoteIdsNeedingIndexUpdateByNotebookId(Integer notebookId) {
    String sql =
        "WITH last_embedding AS (\n"
            + "  SELECT ne.note_id, MAX(ne.updated_at) AS last_updated\n"
            + "  FROM note_embeddings ne\n"
            + "  GROUP BY ne.note_id\n"
            + ")\n"
            + "SELECT n.id\n"
            + "FROM note n\n"
            + "LEFT JOIN last_embedding e ON e.note_id = n.id\n"
            + "WHERE n.notebook_id = ? AND n.deleted_at IS NULL\n"
            + "  AND (e.last_updated IS NULL OR n.updated_at > e.last_updated)";

    return jdbcTemplate.query(sql, ps -> ps.setInt(1, notebookId), (rs, rowNum) -> rs.getInt(1));
  }

  private static String floatsToJson(java.util.List<? extends Number> floats) {
    StringBuilder sb = new StringBuilder(floats.size() * 8);
    sb.append('[');
    for (int i = 0; i < floats.size(); i++) {
      if (i > 0) sb.append(',');
      // Ensure JSON numeric formatting; avoid "NaN"/"Infinity"
      float f = floats.get(i).floatValue();
      if (Float.isFinite(f)) {
        sb.append(Float.toString(f));
      } else {
        sb.append('0');
      }
    }
    sb.append(']');
    return sb.toString();
  }

  private static byte[] floatsToBytes(java.util.List<? extends Number> floats) {
    byte[] bytes = new byte[floats.size() * 4];
    for (int i = 0; i < floats.size(); i++) {
      float f = floats.get(i).floatValue();
      int intBits = Float.floatToIntBits(f);
      // big-endian to be consistent with existing conversion
      bytes[i * 4] = (byte) (intBits >> 24);
      bytes[i * 4 + 1] = (byte) (intBits >> 16);
      bytes[i * 4 + 2] = (byte) (intBits >> 8);
      bytes[i * 4 + 3] = (byte) intBits;
    }
    return bytes;
  }

  private static class Scope {
    final String whereClause;
    final java.util.List<Object> params;

    Scope(String whereClause, java.util.List<Object> params) {
      this.whereClause = whereClause;
      this.params = params;
    }
  }

  private Scope buildScope(
      Integer userId,
      Integer notebookId,
      boolean allMyNotebooksAndSubscriptions,
      boolean allMyCircles) {
    String scopeClause;
    java.util.List<Object> params = new java.util.ArrayList<>();

    if (Boolean.TRUE.equals(allMyCircles)) {
      scopeClause =
          " (o.user_id = ? OR EXISTS (SELECT 1 FROM subscription s WHERE s.user_id = ? AND s.notebook_id = nb.id) "
              + " OR EXISTS (SELECT 1 FROM circle_user cu WHERE cu.user_id = ? AND cu.circle_id = o.circle_id)) ";
      params.add(userId);
      params.add(userId);
      params.add(userId);
    } else if (Boolean.TRUE.equals(allMyNotebooksAndSubscriptions)) {
      scopeClause =
          " (o.user_id = ? OR EXISTS (SELECT 1 FROM subscription s WHERE s.user_id = ? AND s.notebook_id = nb.id)) ";
      params.add(userId);
      params.add(userId);
    } else if (notebookId != null) {
      scopeClause = " (nb.id = ?) ";
      params.add(notebookId);
    } else {
      return null;
    }

    return new Scope(scopeClause, params);
  }
}
