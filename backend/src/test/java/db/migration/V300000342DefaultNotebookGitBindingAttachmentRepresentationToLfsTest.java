package db.migration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.services.notebookGit.NotebookGitJdbcFixture;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link V300000342__DefaultNotebookGitBindingAttachmentRepresentationToLfs} against this
 * worktree's isolated MySQL, starting from the pre-migration {@code 'RAW'} column default.
 */
class V300000342DefaultNotebookGitBindingAttachmentRepresentationToLfsTest {

  private final NotebookGitJdbcFixture jdbc = new NotebookGitJdbcFixture();

  @BeforeEach
  void startBeforeTheMigration() throws SQLException {
    setColumnDefault("RAW");
  }

  @AfterEach
  void cleanUp() throws SQLException {
    setColumnDefault("LFS");
    jdbc.close();
  }

  @Test
  void aRawBindingRefusesNamingItsNotebookAndLeavesTheDefault() throws SQLException {
    int rawBindingId = jdbc.insertBinding("a".repeat(40));

    try (Connection connection = jdbc.openConnection()) {
      IllegalStateException refusal =
          assertThrows(
              IllegalStateException.class,
              () ->
                  V300000342__DefaultNotebookGitBindingAttachmentRepresentationToLfs
                      .defaultToLfsUnlessAnyBindingIsRaw(connection));
      assertThat(
          refusal.getMessage(),
          equalTo(
              "Notebooks still store files as RAW; convert them to LFS first: notebooks "
                  + notebookIdOf(rawBindingId)));
    }
    assertThat(columnDefault(), equalTo("RAW"));
  }

  @Test
  void withoutRawBindingsNewBindingsStoreLfsFromTheDefault() throws SQLException {
    try (Connection connection = jdbc.openConnection()) {
      V300000342__DefaultNotebookGitBindingAttachmentRepresentationToLfs
          .defaultToLfsUnlessAnyBindingIsRaw(connection);
    }

    assertThat(columnDefault(), equalTo("LFS"));
    int newBindingId = jdbc.insertBinding("a".repeat(40));
    assertThat(
        queryString(
            "SELECT attachment_representation FROM notebook_git_binding WHERE id = "
                + newBindingId),
        equalTo("LFS"));
  }

  private String notebookIdOf(int bindingId) throws SQLException {
    return queryString("SELECT notebook_id FROM notebook_git_binding WHERE id = " + bindingId);
  }

  private String columnDefault() throws SQLException {
    return queryString(
        "SELECT COLUMN_DEFAULT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()"
            + " AND TABLE_NAME = 'notebook_git_binding'"
            + " AND COLUMN_NAME = 'attachment_representation'");
  }

  private void setColumnDefault(String value) throws SQLException {
    try (Connection connection = jdbc.openConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(
          "ALTER TABLE notebook_git_binding ALTER COLUMN attachment_representation SET DEFAULT '"
              + value
              + "'");
    }
  }

  private String queryString(String sql) throws SQLException {
    try (Connection connection = jdbc.openConnection();
        Statement statement = connection.createStatement();
        ResultSet row = statement.executeQuery(sql)) {
      row.next();
      return row.getString(1);
    }
  }
}
