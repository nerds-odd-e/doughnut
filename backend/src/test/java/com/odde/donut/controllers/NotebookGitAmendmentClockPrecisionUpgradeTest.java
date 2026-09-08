package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.NotebookGitBinding;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Migration evidence for widening {@code amendment_last_changed_at} and freezing prior eligibility.
 * Ordinary amendment batching stays covered by {@link
 * NotebookGitWebContentAmendmentControllerTest}.
 */
class NotebookGitAmendmentClockPrecisionUpgradeTest
    extends NotebookGitWebContentAmendmentControllerTestSupport {

  private static final String PRE_MIGRATION_DDL =
      """
      CREATE TEMPORARY TABLE amendment_clock_upgrade_fixture (
        id int unsigned NOT NULL,
        accepted_git_object_id varchar(40) NOT NULL,
        bundle_bytes mediumblob NOT NULL,
        amendment_head varchar(40) NULL,
        amendment_note_id int unsigned NULL,
        amendment_last_changed_at timestamp NULL,
        PRIMARY KEY (id)
      )
      """;

  private static final String APPLY_PRECISION_AND_FREEZE =
      """
      ALTER TABLE amendment_clock_upgrade_fixture
        MODIFY COLUMN amendment_last_changed_at timestamp(3) NULL;
      UPDATE amendment_clock_upgrade_fixture
      SET
        amendment_head = NULL,
        amendment_note_id = NULL,
        amendment_last_changed_at = NULL
      WHERE amendment_head IS NOT NULL
         OR amendment_note_id IS NOT NULL
         OR amendment_last_changed_at IS NOT NULL
      """;

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void appliedSchemaStoresFractionalAmendmentClock() {
    String columnType =
        jdbcTemplate.queryForObject(
            """
            SELECT COLUMN_TYPE FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'notebook_git_binding'
              AND column_name = 'amendment_last_changed_at'
            """,
            String.class);
    assertThat(columnType, equalToIgnoringCase("timestamp(3)"));
  }

  @Test
  void preMigrationEligibleAndFrozenBindingsKeepBytesAndBecomeOrRemainFrozen() {
    jdbcTemplate.execute(PRE_MIGRATION_DDL);
    byte[] eligibleBundle = new byte[] {1, 2, 3, 4};
    byte[] frozenBundle = new byte[] {5, 6, 7, 8};
    jdbcTemplate.update(
        """
        INSERT INTO amendment_clock_upgrade_fixture
          (id, accepted_git_object_id, bundle_bytes,
           amendment_head, amendment_note_id, amendment_last_changed_at)
        VALUES
          (1, ?, ?, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 42, '2026-09-08 10:00:01'),
          (2, ?, ?, NULL, NULL, NULL)
        """,
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        eligibleBundle,
        "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
        frozenBundle);

    for (String statement : APPLY_PRECISION_AND_FREEZE.strip().split(";\\s*")) {
      if (!statement.isBlank()) {
        jdbcTemplate.execute(statement);
      }
    }

    record FixtureRow(
        int id,
        String acceptedHead,
        byte[] bundleBytes,
        String amendmentHead,
        Integer amendmentNoteId,
        java.sql.Timestamp amendmentLastChangedAt) {}

    java.util.List<FixtureRow> rows =
        jdbcTemplate.query(
            """
            SELECT id, accepted_git_object_id, bundle_bytes,
                   amendment_head, amendment_note_id, amendment_last_changed_at
            FROM amendment_clock_upgrade_fixture ORDER BY id
            """,
            (rs, rowNum) ->
                new FixtureRow(
                    rs.getInt("id"),
                    rs.getString("accepted_git_object_id"),
                    rs.getBytes("bundle_bytes"),
                    rs.getString("amendment_head"),
                    (Integer) rs.getObject("amendment_note_id"),
                    rs.getTimestamp("amendment_last_changed_at")));

    assertThat(rows.size(), is(2));
    assertThat(rows.get(0).id(), is(1));
    assertThat(rows.get(0).acceptedHead(), equalTo("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
    assertThat(Arrays.equals(rows.get(0).bundleBytes(), eligibleBundle), is(true));
    assertThat(rows.get(0).amendmentHead(), nullValue());
    assertThat(rows.get(0).amendmentNoteId(), nullValue());
    assertThat(rows.get(0).amendmentLastChangedAt(), nullValue());
    assertThat(rows.get(1).id(), is(2));
    assertThat(rows.get(1).acceptedHead(), equalTo("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"));
    assertThat(Arrays.equals(rows.get(1).bundleBytes(), frozenBundle), is(true));
    assertThat(rows.get(1).amendmentHead(), nullValue());
    assertThat(rows.get(1).amendmentNoteId(), nullValue());
    assertThat(rows.get(1).amendmentLastChangedAt(), nullValue());
  }

  @Test
  void afterUpgradeFreezeChangedSaveAppendsAndEstablishesPreciseEligibility() throws Exception {
    Fixture fixture = fixture("UpgradeAppend");
    saveAt(fixture.noteId(), content("pre-upgrade"), T1000);
    NotebookGitBinding beforeFreeze =
        inCommittedTransaction(
            transactionManager,
            () ->
                notebookGitBindingRepository.findByNotebook_Id(fixture.notebookId()).orElseThrow());
    String head = beforeFreeze.getAcceptedGitObjectId();
    byte[] bundle = beforeFreeze.getBundleBytes();

    jdbcTemplate.update(
        """
        UPDATE notebook_git_binding
        SET
          amendment_head = NULL,
          amendment_note_id = NULL,
          amendment_last_changed_at = NULL
        WHERE notebook_id = ?
        """,
        fixture.notebookId());

    NotebookGitBinding frozen =
        inCommittedTransaction(
            transactionManager,
            () ->
                notebookGitBindingRepository.findByNotebook_Id(fixture.notebookId()).orElseThrow());
    assertThat(frozen.getAcceptedGitObjectId(), equalTo(head));
    assertThat(frozen.getBundleBytes(), equalTo(bundle));
    assertThat(frozen.getAmendmentHead(), nullValue());
    assertThat(frozen.getAmendmentNoteId(), nullValue());
    assertThat(frozen.getAmendmentLastChangedAt(), nullValue());

    saveAt(fixture.noteId(), content("post-upgrade"), T1000_600);
    assertThat(contentChain(fixture.notebookId(), "UpgradeAppend.md").size(), is(3));

    NotebookGitBinding afterSave =
        inCommittedTransaction(
            transactionManager,
            () ->
                notebookGitBindingRepository.findByNotebook_Id(fixture.notebookId()).orElseThrow());
    assertThat(afterSave.getAmendmentNoteId(), equalTo(fixture.noteId()));
    assertThat(afterSave.getAmendmentLastChangedAt().toInstant(), equalTo(T1000_600));
  }
}
