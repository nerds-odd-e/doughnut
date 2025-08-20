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

  public void insert(Integer noteId, String kind, java.util.List<Float> embeddingFloats) {
    if (isVectorColumn()) {
      // GCP Cloud SQL VECTOR column: use string_to_vector(JSON)
      String json = floatsToJson(embeddingFloats);
      String sql =
          "INSERT INTO note_embeddings (note_id, kind, created_at, updated_at, "
              + embeddingColumn()
              + ") VALUES (?, ?, NOW(), NOW(), string_to_vector(?))";
      jdbcTemplate.update(
          sql,
          (PreparedStatementSetter)
              ps -> {
                ps.setInt(1, noteId);
                ps.setString(2, kind);
                ps.setString(3, json);
              });
    } else {
      // Local VARBINARY column
      byte[] bytes = floatsToBytes(embeddingFloats);
      String sql =
          "INSERT INTO note_embeddings (note_id, kind, created_at, updated_at, "
              + embeddingColumn()
              + ") VALUES (?, ?, NOW(), NOW(), ?)";
      jdbcTemplate.update(
          sql,
          (PreparedStatementSetter)
              ps -> {
                ps.setInt(1, noteId);
                ps.setString(2, kind);
                ps.setBytes(3, bytes);
              });
    }
  }

  public Optional<byte[]> select(Integer noteId, String kind) {
    if (isVectorColumn()) {
      // Not needed for current flows; return empty to avoid relying on vector-to-bytes conversion
      return Optional.empty();
    }
    String sql =
        "SELECT "
            + embeddingColumn()
            + " FROM note_embeddings WHERE note_id=? AND kind=? ORDER BY id DESC LIMIT 1";
    try {
      byte[] bytes =
          jdbcTemplate.queryForObject(
              sql,
              (rs, rowNum) -> rs.getBytes(1),
              noteId,
              kind);
      return Optional.ofNullable(bytes);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  private static String floatsToJson(java.util.List<Float> floats) {
    StringBuilder sb = new StringBuilder(floats.size() * 8);
    sb.append('[');
    for (int i = 0; i < floats.size(); i++) {
      if (i > 0) sb.append(',');
      // Ensure JSON numeric formatting; avoid "NaN"/"Infinity"
      float f = floats.get(i);
      if (Float.isFinite(f)) {
        sb.append(Float.toString(f));
      } else {
        sb.append('0');
      }
    }
    sb.append(']');
    return sb.toString();
  }

  private static byte[] floatsToBytes(java.util.List<Float> floats) {
    byte[] bytes = new byte[floats.size() * 4];
    for (int i = 0; i < floats.size(); i++) {
      int intBits = Float.floatToIntBits(floats.get(i));
      // big-endian to be consistent with existing conversion
      bytes[i * 4] = (byte) (intBits >> 24);
      bytes[i * 4 + 1] = (byte) (intBits >> 16);
      bytes[i * 4 + 2] = (byte) (intBits >> 8);
      bytes[i * 4 + 3] = (byte) intBits;
    }
    return bytes;
  }
}


