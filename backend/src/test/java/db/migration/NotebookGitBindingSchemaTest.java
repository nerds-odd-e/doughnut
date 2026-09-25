package db.migration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.services.notebookGit.NotebookGitJdbcFixture;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The migrated {@code notebook_git_binding} table in this worktree's isolated MySQL. */
class NotebookGitBindingSchemaTest {

  private final NotebookGitJdbcFixture jdbc = new NotebookGitJdbcFixture();

  @AfterEach
  void cleanUp() throws SQLException {
    jdbc.close();
  }

  @Test
  void noLongerRecordsHowANotebookStoresFiles() throws SQLException {
    try (Connection connection = jdbc.openConnection();
        Statement statement = connection.createStatement();
        ResultSet row =
            statement.executeQuery(
                "SELECT COUNT(*) FROM information_schema.COLUMNS"
                    + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notebook_git_binding'"
                    + " AND COLUMN_NAME = 'attachment_representation'")) {
      row.next();
      assertThat(row.getInt(1), equalTo(0));
    }
  }
}
