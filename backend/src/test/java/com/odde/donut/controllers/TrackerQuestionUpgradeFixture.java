package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;

import jakarta.persistence.EntityManager;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.flywaydb.core.Flyway;
import org.hibernate.Session;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

final class TrackerQuestionUpgradeFixture implements AutoCloseable {
  final String catalog = "question_upgrade_" + UUID.randomUUID().toString().replace("-", "");
  final JdbcTemplate jdbc;
  private final JdbcTemplate admin;
  private final Flyway applicationFlyway;
  private final EntityManager entityManager;
  private final PlatformTransactionManager transactionManager;
  private final String account;
  private boolean created;
  private boolean granted;

  TrackerQuestionUpgradeFixture(
      Flyway applicationFlyway,
      EntityManager entityManager,
      PlatformTransactionManager transactionManager)
      throws Exception {
    this.applicationFlyway = applicationFlyway;
    this.entityManager = entityManager;
    this.transactionManager = transactionManager;
    String sourceUrl;
    try (Connection source = applicationFlyway.getConfiguration().getDataSource().getConnection();
        var statement = source.createStatement();
        ResultSet row = statement.executeQuery("SELECT CURRENT_USER()")) {
      sourceUrl = source.getMetaData().getURL();
      row.next();
      String identity = row.getString(1);
      int separator = identity.lastIndexOf('@');
      account =
          quoted(identity.substring(0, separator))
              + "@"
              + quoted(identity.substring(separator + 1));
    }
    String password = System.getenv().getOrDefault("QUESTION_UPGRADE_DB_ADMIN_PASSWORD", "");
    admin = new JdbcTemplate(new DriverManagerDataSource(sourceUrl, "root", password));
    URI source = URI.create(sourceUrl.substring("jdbc:".length()));
    String targetUrl =
        "jdbc:"
            + new URI(
                source.getScheme(),
                source.getUserInfo(),
                source.getHost(),
                source.getPort(),
                "/" + catalog,
                source.getQuery(),
                source.getFragment());
    jdbc = new JdbcTemplate(new DriverManagerDataSource(targetUrl, "root", password));
    try {
      admin.execute(
          "CREATE DATABASE `" + catalog + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
      created = true;
      admin.execute("GRANT ALL PRIVILEGES ON `" + catalog + "`.* TO " + account);
      granted = true;
      migrations("300000357").migrate();
    } catch (Exception failure) {
      try {
        close();
      } catch (Exception cleanup) {
        failure.addSuppressed(cleanup);
      }
      throw failure;
    }
  }

  <T> T withTargetCatalog(Supplier<T> arrange, Consumer<T> verifyBeforeCommit) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          entityManager.clear();
          return entityManager
              .unwrap(Session.class)
              .doReturningWork(connection -> {
                String original = connection.getCatalog();
                connection.setCatalog(catalog);
                try {
                  T result = arrange.get();
                  entityManager.flush();
                  verifyBeforeCommit.accept(result);
                  return result;
                } finally {
                  entityManager.clear();
                  connection.setCatalog(original);
                }
              });
        });
  }

  Flyway migrations(String target) {
    return Flyway.configure()
        .configuration(applicationFlyway.getConfiguration())
        .dataSource(jdbc.getDataSource())
        .schemas(catalog)
        .defaultSchema(catalog)
        .target(target)
        .load();
  }

  Map<String, List<Map<String, Object>>> snapshot(Map<String, Integer> graph) {
    Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
    graph.forEach((table, id) ->
        result.put(table, jdbc.queryForList("SELECT * FROM " + table + " WHERE id = ?", id)));
    return result;
  }

  @Override
  public void close() {
    RuntimeException failure = null;
    if (granted) {
      try {
        admin.execute("REVOKE ALL PRIVILEGES ON `" + catalog + "`.* FROM " + account);
        granted = false;
      } catch (RuntimeException problem) {
        failure = problem;
      }
    }
    if (created) {
      try {
        admin.execute("DROP DATABASE `" + catalog + "`");
        created = false;
      } catch (RuntimeException problem) {
        if (failure == null) {
          failure = problem;
        } else {
          failure.addSuppressed(problem);
        }
      }
    }
    if (failure != null) {
      throw failure;
    }
  }

  private static String quoted(String value) {
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'";
  }
}
