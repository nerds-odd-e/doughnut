package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import com.odde.donut.entities.Note;
import com.odde.donut.testability.MakeMe;
import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteLevelIndexBackfillTest {

  private static final String FENCED_WITHOUT_LEVEL =
      """
      ---
      type: Note
      ---

      body
      """;

  @Autowired MakeMe makeMe;
  @Autowired EntityManager entityManager;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5, 6})
  void inserts_note_level_and_cache_from_legacy_column(int level) throws Exception {
    Note note = makeMe.aNote().content(FENCED_WITHOUT_LEVEL).level(level).please();

    runBackfill();

    assertThat(noteContent(note), containsString("note_level: " + level));
    assertThat(cacheLevels(note), equalTo(List.of(level)));
  }

  @Test
  void leaves_level_zero_notes_without_key_or_cache() throws Exception {
    Note note = makeMe.aNote().content(FENCED_WITHOUT_LEVEL).level(0).please();

    runBackfill();

    assertThat(noteContent(note), equalTo(FENCED_WITHOUT_LEVEL));
    assertThat(cacheLevels(note), empty());
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 7})
  void omits_out_of_range_legacy_level(int level) throws Exception {
    Note note = makeMe.aNote().content(FENCED_WITHOUT_LEVEL).level(level).please();

    runBackfill();

    assertThat(noteContent(note), not(containsString("note_level:")));
    assertThat(cacheLevels(note), empty());
  }

  @Test
  void keeps_valid_yaml_and_caches_from_frontmatter_not_column() throws Exception {
    String yamlWins =
        """
        ---
        type: Note
        note_level: 2
        ---

        body
        """;
    Note note = makeMe.aNote().content(yamlWins).level(5).please();

    runBackfill();

    assertThat(noteContent(note), equalTo(yamlWins));
    assertThat(cacheLevels(note), equalTo(List.of(2)));
  }

  @Test
  void skips_soft_deleted_notes() throws Exception {
    Note note = makeMe.aNote().content(FENCED_WITHOUT_LEVEL).level(3).softDeleted().please();

    runBackfill();

    assertThat(noteContent(note), equalTo(FENCED_WITHOUT_LEVEL));
    assertThat(cacheLevels(note), empty());
  }

  @Test
  void inserts_note_level_when_existing_yaml_key_is_invalid() throws Exception {
    String invalid =
        """
        ---
        type: Note
        note_level: 0
        ---

        body
        """;
    Note note = makeMe.aNote().content(invalid).level(3).please();

    runBackfill();

    assertThat(noteContent(note), containsString("note_level: 3"));
    assertThat(cacheLevels(note), equalTo(List.of(3)));
  }

  @Test
  void inserts_key_verbatim_without_redumping_existing_yaml() throws Exception {
    String authored =
        """
        ---
        # keep this comment
        topic: "physics"
        ---

        body
        """;
    Note note = makeMe.aNote().content(authored).level(4).please();

    runBackfill();

    String content = noteContent(note);
    assertThat(content, containsString("# keep this comment"));
    assertThat(content, containsString("topic: \"physics\""));
    assertThat(content, containsString("note_level: 4"));
  }

  private void runBackfill() throws Exception {
    entityManager.flush();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      NoteLevelIndexBackfill.run(connection);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private String noteContent(Note note) {
    return jdbcTemplate.queryForObject(
        "SELECT content FROM note WHERE id = ?", String.class, note.getId());
  }

  private List<Integer> cacheLevels(Note note) {
    return jdbcTemplate.query(
        "SELECT level FROM note_level_index WHERE note_id = ?",
        (rs, rowNum) -> rs.getInt("level"),
        note.getId());
  }
}
