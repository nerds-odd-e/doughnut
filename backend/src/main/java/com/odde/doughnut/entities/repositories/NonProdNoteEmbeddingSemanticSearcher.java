package com.odde.doughnut.entities.repositories;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;

class NonProdNoteEmbeddingSemanticSearcher {
  private static final float MISSING_DIST = 1e9f;
  private static final int NON_PROD_CANDIDATE_CAP_NOTES = 500;

  List<NoteEmbeddingJdbcRepository.SimilarNoteRow> search(
      JdbcTemplate jdbcTemplate,
      String embeddingColumnName,
      String scopeWhereClause,
      List<Object> scopeParams,
      List<? extends Number> queryEmbedding,
      int limit,
      float maxCombinedDistance) {
    int candidateRowsLimit = NON_PROD_CANDIDATE_CAP_NOTES * 2; // title + details

    String sql =
        "WITH latest AS (\n"
            + "  SELECT ne.note_id, ne.kind, "
            + embeddingColumnName
            + " AS emb,\n"
            + "         ROW_NUMBER() OVER (PARTITION BY ne.note_id, ne.kind ORDER BY ne.updated_at DESC) rn\n"
            + "  FROM note_embeddings ne\n"
            + "  JOIN note n ON n.id = ne.note_id AND n.deleted_at IS NULL\n"
            + "  JOIN notebook nb ON nb.id = n.notebook_id AND nb.deleted_at IS NULL\n"
            + "  LEFT JOIN ownership o ON o.id = nb.ownership_id\n"
            + "  WHERE "
            + scopeWhereClause
            + "\n)\n"
            + "SELECT note_id, kind, emb FROM latest WHERE rn = 1 LIMIT ?";

    List<Object> params = new ArrayList<>(scopeParams);
    params.add(candidateRowsLimit);

    List<NonProdEmbeddingRow> rows =
        jdbcTemplate.query(
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
                new NonProdEmbeddingRow(
                    rs.getInt("note_id"), rs.getString("kind"), rs.getBytes("emb")));

    float[] query = toFloatArray(queryEmbedding);
    Map<Integer, Float> titleDistances = new HashMap<>();
    Map<Integer, Float> detailsDistances = new HashMap<>();

    for (NonProdEmbeddingRow r : rows) {
      if (r.bytes == null || r.bytes.length == 0) continue;
      float[] vec = bytesToFloats(r.bytes);
      if (vec.length != query.length) continue; // skip mismatched dimensions
      float dist = l2SquaredDistance(query, vec);
      if ("TITLE".equals(r.kind)) {
        titleDistances.put(r.noteId, dist);
      } else if ("DETAILS".equals(r.kind)) {
        detailsDistances.put(r.noteId, dist);
      }
    }

    Set<Integer> noteIds = new HashSet<>();
    noteIds.addAll(titleDistances.keySet());
    noteIds.addAll(detailsDistances.keySet());

    List<NoteEmbeddingJdbcRepository.SimilarNoteRow> result = new ArrayList<>(noteIds.size());
    for (Integer nid : noteIds) {
      Float td = titleDistances.get(nid);
      Float dd = detailsDistances.get(nid);
      float combined = combinedDistance(td, dd);
      result.add(
          new NoteEmbeddingJdbcRepository.SimilarNoteRow(
              nid, td == null ? MISSING_DIST : td, dd == null ? MISSING_DIST : dd, combined));
    }

    result.removeIf(r -> r.combinedDist > maxCombinedDistance);
    result.sort(java.util.Comparator.comparingDouble(r -> r.combinedDist));
    if (result.size() > limit) {
      return result.subList(0, limit);
    }
    return result;
  }

  private static float[] toFloatArray(List<? extends Number> floats) {
    float[] arr = new float[floats.size()];
    for (int i = 0; i < floats.size(); i++) {
      arr[i] = floats.get(i).floatValue();
    }
    return arr;
  }

  private static float[] bytesToFloats(byte[] bytes) {
    int len = bytes.length / 4;
    float[] out = new float[len];
    for (int i = 0; i < len; i++) {
      int b0 = (bytes[i * 4] & 0xFF) << 24;
      int b1 = (bytes[i * 4 + 1] & 0xFF) << 16;
      int b2 = (bytes[i * 4 + 2] & 0xFF) << 8;
      int b3 = (bytes[i * 4 + 3] & 0xFF);
      int intBits = b0 | b1 | b2 | b3;
      out[i] = Float.intBitsToFloat(intBits);
    }
    return out;
  }

  private static float l2SquaredDistance(float[] a, float[] b) {
    int n = a.length;
    float sum = 0f;
    for (int i = 0; i < n; i++) {
      float d = a[i] - b[i];
      sum += d * d;
    }
    return sum;
  }

  private static float combinedDistance(Float titleDist, Float detailsDist) {
    float td = titleDist == null ? MISSING_DIST : titleDist;
    float dd = detailsDist == null ? MISSING_DIST : detailsDist;
    return (td * 2f + dd) / 3f;
  }

  private static class NonProdEmbeddingRow {
    final Integer noteId;
    final String kind;
    final byte[] bytes;

    NonProdEmbeddingRow(Integer noteId, String kind, byte[] bytes) {
      this.noteId = noteId;
      this.kind = kind;
      this.bytes = bytes;
    }
  }
}
