package com.odde.doughnut.configs;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guards hard-delete roots: every foreign key reachable via CASCADE from a declared root must not
 * use NO ACTION / RESTRICT (unless explicitly allowlisted). SET NULL terminates a branch.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DeletableEntityFkClosureTest {

  private static final List<String> HARD_DELETABLE_ROOTS = List.of("memory_tracker");

  /**
   * Deliberate RESTRICT / NO ACTION edges inside a hard-deletable subtree. Each entry must carry a
   * reason so the exception is a conscious product decision, not silent drift.
   */
  private static final List<AllowedRestrictingFk> ALLOWED_RESTRICTING_FKS = List.of();

  private static final String FOREIGN_KEYS_BY_PARENT_SQL =
      """
      SELECT k.TABLE_NAME AS child_table,
             GROUP_CONCAT(k.COLUMN_NAME ORDER BY k.ORDINAL_POSITION) AS child_columns,
             k.REFERENCED_TABLE_NAME AS parent_table,
             r.CONSTRAINT_NAME AS constraint_name,
             r.DELETE_RULE AS delete_rule
      FROM information_schema.KEY_COLUMN_USAGE k
      JOIN information_schema.REFERENTIAL_CONSTRAINTS r
        ON k.CONSTRAINT_SCHEMA = r.CONSTRAINT_SCHEMA
       AND k.CONSTRAINT_NAME = r.CONSTRAINT_NAME
      WHERE k.TABLE_SCHEMA = DATABASE()
        AND k.REFERENCED_TABLE_NAME IS NOT NULL
      GROUP BY k.TABLE_NAME, k.REFERENCED_TABLE_NAME, r.CONSTRAINT_NAME, r.DELETE_RULE
      """;

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void noRestrictingForeignKeysInHardDeletableClosure() {
    Map<String, List<ForeignKeyEdge>> foreignKeysByReferencedTable =
        loadForeignKeysByReferencedTable();
    Set<String> violations = new HashSet<>();

    for (String root : HARD_DELETABLE_ROOTS) {
      walkClosure(root, List.of(root), new HashSet<>(), foreignKeysByReferencedTable, violations);
    }

    assertTrue(
        violations.isEmpty(),
        () ->
            "Restricting foreign keys block hard delete. Paths show root -> … (CASCADE) -> child.column"
                + " [constraint] RULE. Allowlist deliberate exceptions in ALLOWED_RESTRICTING_FKS.\n"
                + String.join("\n", violations));
  }

  private Map<String, List<ForeignKeyEdge>> loadForeignKeysByReferencedTable() {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList(FOREIGN_KEYS_BY_PARENT_SQL);

    Map<String, List<ForeignKeyEdge>> byParent = new HashMap<>();
    for (Map<String, Object> row : rows) {
      ForeignKeyEdge edge =
          new ForeignKeyEdge(
              (String) row.get("child_table"),
              (String) row.get("child_columns"),
              (String) row.get("parent_table"),
              (String) row.get("constraint_name"),
              (String) row.get("delete_rule"));
      byParent.computeIfAbsent(edge.parentTable(), ignored -> new ArrayList<>()).add(edge);
    }
    return byParent;
  }

  private void walkClosure(
      String table,
      List<String> pathSegments,
      Set<String> visitedTables,
      Map<String, List<ForeignKeyEdge>> foreignKeysByReferencedTable,
      Set<String> violations) {
    if (!visitedTables.add(table)) {
      return;
    }

    for (ForeignKeyEdge edge : foreignKeysByReferencedTable.getOrDefault(table, List.of())) {
      if (isAllowlisted(edge)) {
        continue;
      }

      switch (edge.deleteRule()) {
        case "CASCADE" -> {
          List<String> extendedPath = new ArrayList<>(pathSegments);
          extendedPath.add(edge.childTable() + " (CASCADE)");
          walkClosure(
              edge.childTable(),
              extendedPath,
              visitedTables,
              foreignKeysByReferencedTable,
              violations);
        }
        case "SET NULL" -> {}
        default -> violations.add(formatViolation(pathSegments, edge));
      }
    }
  }

  private static boolean isAllowlisted(ForeignKeyEdge edge) {
    return ALLOWED_RESTRICTING_FKS.stream().anyMatch(allowed -> allowed.matches(edge));
  }

  private static String formatViolation(List<String> pathSegments, ForeignKeyEdge edge) {
    List<String> segments = new ArrayList<>(pathSegments);
    segments.add(
        edge.childTable()
            + "."
            + edge.childColumns()
            + " ["
            + edge.constraintName()
            + "] "
            + edge.deleteRule());
    return String.join(" -> ", segments);
  }

  private record ForeignKeyEdge(
      String childTable,
      String childColumns,
      String parentTable,
      String constraintName,
      String deleteRule) {}

  private record AllowedRestrictingFk(
      String childTable, String parentTable, String constraintName, String reason) {

    boolean matches(ForeignKeyEdge edge) {
      return childTable.equals(edge.childTable())
          && parentTable.equals(edge.parentTable())
          && constraintName.equals(edge.constraintName());
    }
  }
}
