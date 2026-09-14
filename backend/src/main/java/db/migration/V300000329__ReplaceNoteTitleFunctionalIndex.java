package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V300000329__ReplaceNoteTitleFunctionalIndex extends BaseJavaMigration {

  private static final String INDEX_NAME = "uk_note_notebook_folder_title";
  private static final List<String> GENERATED_COLUMN_NAMES =
      List.of("title_uniqueness_notebook_id", "title_uniqueness_folder_id", "title_uniqueness_key");
  private static final List<String> ORIGINAL_EXPRESSIONS =
      List.of("ifnull(notebook_id,0)", "ifnull(folder_id,0)", "lower(title)");

  @Override
  public boolean canExecuteInTransaction() {
    return false;
  }

  @Override
  public void migrate(Context context) throws SQLException {
    Connection connection = context.getConnection();
    List<GeneratedColumn> generatedColumns = generatedColumns(connection);
    List<IndexPart> indexParts = indexParts(connection);
    ForeignKey folderForeignKey = folderForeignKey(connection);

    if (isConverted(generatedColumns, indexParts, folderForeignKey)) {
      return;
    }
    if (!generatedColumns.isEmpty()
        || !isOriginalFunctionalIndex(indexParts)
        || !(folderForeignKey.hasDeleteRule("SET NULL") || folderForeignKey.isMissing())) {
      throw new SQLException(
          "V300000329: unrecognized note title uniqueness columns/index/folder foreign key schema"
              + " state");
    }

    try (Statement statement = connection.createStatement()) {
      if (!folderForeignKey.isMissing()) {
        statement.executeUpdate("ALTER TABLE note DROP FOREIGN KEY fk_note_folder");
      }
      statement.executeUpdate(
          """
          ALTER TABLE note
            DROP INDEX uk_note_notebook_folder_title,
            ADD COLUMN title_uniqueness_notebook_id INT UNSIGNED
              GENERATED ALWAYS AS (IFNULL(notebook_id, 0)) STORED,
            ADD COLUMN title_uniqueness_folder_id INT UNSIGNED
              GENERATED ALWAYS AS (IFNULL(folder_id, 0)) STORED,
            ADD COLUMN title_uniqueness_key VARCHAR(150)
              CHARACTER SET utf8mb4 COLLATE utf8mb4_bin
              GENERATED ALWAYS AS (LOWER(title)) STORED,
            ADD UNIQUE INDEX uk_note_notebook_folder_title (
              title_uniqueness_notebook_id,
              title_uniqueness_folder_id,
              title_uniqueness_key
            ),
            ADD CONSTRAINT fk_note_folder FOREIGN KEY (folder_id) REFERENCES folder (id)
              ON DELETE RESTRICT
          """);
    }
  }

  private static List<GeneratedColumn> generatedColumns(Connection connection) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            """
            SELECT column_name, column_type, collation_name, extra, generation_expression
            FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'note'
              AND column_name IN (?, ?, ?)
            ORDER BY ordinal_position
            """)) {
      for (int index = 0; index < GENERATED_COLUMN_NAMES.size(); index++) {
        statement.setString(index + 1, GENERATED_COLUMN_NAMES.get(index));
      }
      try (ResultSet result = statement.executeQuery()) {
        List<GeneratedColumn> columns = new ArrayList<>();
        while (result.next()) {
          columns.add(
              new GeneratedColumn(
                  result.getString("column_name"),
                  result.getString("column_type"),
                  result.getString("collation_name"),
                  result.getString("extra"),
                  normalizedExpression(result.getString("generation_expression"))));
        }
        return columns;
      }
    }
  }

  private static List<IndexPart> indexParts(Connection connection) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            """
            SELECT column_name, expression, non_unique
            FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'note' AND index_name = ?
            ORDER BY seq_in_index
            """)) {
      statement.setString(1, INDEX_NAME);
      try (ResultSet result = statement.executeQuery()) {
        List<IndexPart> parts = new ArrayList<>();
        while (result.next()) {
          parts.add(
              new IndexPart(
                  result.getString("column_name"),
                  normalizedExpression(result.getString("expression")),
                  result.getInt("non_unique")));
        }
        return parts;
      }
    }
  }

  private static ForeignKey folderForeignKey(Connection connection) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            """
            SELECT k.column_name, k.referenced_table_name, k.referenced_column_name,
                   r.delete_rule, r.update_rule
            FROM information_schema.key_column_usage k
            JOIN information_schema.referential_constraints r
              ON r.constraint_schema = k.constraint_schema
             AND r.table_name = k.table_name
             AND r.constraint_name = k.constraint_name
            WHERE k.constraint_schema = DATABASE() AND k.table_name = 'note'
              AND k.constraint_name = 'fk_note_folder'
            """)) {
      try (ResultSet result = statement.executeQuery()) {
        if (!result.next()) {
          return ForeignKey.NONE;
        }
        ForeignKey foreignKey =
            new ForeignKey(
                result.getString("column_name"),
                result.getString("referenced_table_name"),
                result.getString("referenced_column_name"),
                result.getString("delete_rule"),
                result.getString("update_rule"));
        if (result.next()) {
          throw new SQLException("V300000329: fk_note_folder has multiple key columns");
        }
        return foreignKey;
      }
    }
  }

  private static boolean isOriginalFunctionalIndex(List<IndexPart> indexParts) {
    if (indexParts.size() != ORIGINAL_EXPRESSIONS.size()) {
      return false;
    }
    for (int index = 0; index < indexParts.size(); index++) {
      IndexPart part = indexParts.get(index);
      if (part.nonUnique() != 0
          || part.columnName() != null
          || !ORIGINAL_EXPRESSIONS.get(index).equals(part.expression())) {
        return false;
      }
    }
    return true;
  }

  private static boolean isConverted(
      List<GeneratedColumn> generatedColumns,
      List<IndexPart> indexParts,
      ForeignKey folderForeignKey) {
    if (!generatedColumns.equals(
        List.of(
            new GeneratedColumn(
                "title_uniqueness_notebook_id",
                "int unsigned",
                null,
                "STORED GENERATED",
                "ifnull(notebook_id,0)"),
            new GeneratedColumn(
                "title_uniqueness_folder_id",
                "int unsigned",
                null,
                "STORED GENERATED",
                "ifnull(folder_id,0)"),
            new GeneratedColumn(
                "title_uniqueness_key",
                "varchar(150)",
                "utf8mb4_bin",
                "STORED GENERATED",
                "lower(title)")))) {
      return false;
    }
    if (indexParts.size() != GENERATED_COLUMN_NAMES.size()
        || !folderForeignKey.hasDeleteRule("RESTRICT")) {
      return false;
    }
    for (int index = 0; index < indexParts.size(); index++) {
      IndexPart part = indexParts.get(index);
      if (part.nonUnique() != 0
          || part.expression() != null
          || !GENERATED_COLUMN_NAMES.get(index).equals(part.columnName())) {
        return false;
      }
    }
    return true;
  }

  private static String normalizedExpression(String expression) {
    if (expression == null) {
      return null;
    }
    return expression.replace("`", "").replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
  }

  private record GeneratedColumn(
      String name, String type, String collation, String extra, String expression) {}

  private record IndexPart(String columnName, String expression, int nonUnique) {}

  private record ForeignKey(
      String columnName,
      String referencedTableName,
      String referencedColumnName,
      String deleteRule,
      String updateRule) {

    private static final ForeignKey NONE = new ForeignKey(null, null, null, null, null);

    private boolean hasDeleteRule(String expectedDeleteRule) {
      return "folder_id".equals(columnName)
          && "folder".equals(referencedTableName)
          && "id".equals(referencedColumnName)
          && expectedDeleteRule.equals(deleteRule)
          && "NO ACTION".equals(updateRule);
    }

    private boolean isMissing() {
      return this.equals(NONE);
    }
  }
}
