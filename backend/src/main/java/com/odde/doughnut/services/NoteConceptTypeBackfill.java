package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.NoteConceptType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class NoteConceptTypeBackfill {

  private NoteConceptTypeBackfill() {}

  public static void run(Connection connection, String gate) throws SQLException {
    if ("1=0".equals(gate)) {
      return;
    }
    if (!"1=1".equals(gate)) {
      throw new IllegalStateException(
          "Note concept type backfill gate must be 1=0 or 1=1, got: " + gate);
    }

    List<Row> rows = new ArrayList<>();
    try (PreparedStatement select = connection.prepareStatement("SELECT id, content FROM note");
        ResultSet rs = select.executeQuery()) {
      while (rs.next()) {
        rows.add(new Row(rs.getInt("id"), rs.getString("content")));
      }
    }

    try (PreparedStatement update =
        connection.prepareStatement("UPDATE note SET content = ? WHERE id = ?")) {
      boolean pending = false;
      for (Row row : rows) {
        String updated = NoteConceptType.ensureStoredType(row.content());
        if (Objects.equals(updated, row.content())) {
          continue;
        }
        update.setString(1, updated);
        update.setInt(2, row.id());
        update.addBatch();
        pending = true;
      }
      if (pending) {
        update.executeBatch();
      }
    }
  }

  private record Row(int id, String content) {}
}
