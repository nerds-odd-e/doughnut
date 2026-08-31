package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.NoteLevelIndex;
import com.odde.donut.entities.repositories.NoteLevelIndexRepository;
import com.odde.donut.testability.MakeMe;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteLevelIndexServiceTest {

  @Autowired MakeMe makeMe;
  @Autowired NoteLevelIndexService noteLevelIndexService;
  @Autowired NoteLevelIndexRepository noteLevelIndexRepository;

  private List<NoteLevelIndex> levelRows(Note note) {
    return noteLevelIndexRepository.findById(note.getId()).stream().toList();
  }

  private Note noteWithContent(String content) {
    return makeMe.aNote().content(content).please();
  }

  @Nested
  class refreshForNote {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    void upserts_valid_integer_note_level(int level) {
      Note note = noteWithContent("---\nnote_level: " + level + "\n---\n\nbody");

      noteLevelIndexService.refreshForNote(note);

      List<NoteLevelIndex> rows = levelRows(note);
      assertThat(rows, hasSize(1));
      assertThat(rows.getFirst().getLevel(), equalTo(level));
    }

    @Test
    void upserts_quoted_digit_string() {
      Note note = noteWithContent("---\nnote_level: \"3\"\n---\n\nbody");

      noteLevelIndexService.refreshForNote(note);

      assertThat(levelRows(note).getFirst().getLevel(), equalTo(3));
    }

    @Test
    void replaces_previous_level_on_second_refresh() {
      Note note = noteWithContent("---\nnote_level: 2\n---\n\n");

      noteLevelIndexService.refreshForNote(note);
      assertThat(levelRows(note), hasSize(1));

      note.setContent("---\nnote_level: 5\n---\n\n");
      makeMe.entityPersister.merge(note);
      noteLevelIndexService.refreshForNote(note);

      List<NoteLevelIndex> rows = levelRows(note);
      assertThat(rows, hasSize(1));
      assertThat(rows.getFirst().getLevel(), equalTo(5));
    }

    @Test
    void leaves_no_row_when_note_level_is_absent() {
      Note note = noteWithContent("---\ntopic: physics\n---\n\nbody");

      noteLevelIndexService.refreshForNote(note);

      assertThat(levelRows(note), empty());
    }

    @Test
    void deletes_row_when_note_level_key_is_removed() {
      Note note = noteWithContent("---\nnote_level: 2\n---\n\nbody");

      noteLevelIndexService.refreshForNote(note);
      assertThat(levelRows(note), hasSize(1));

      note.setContent("---\ntopic: physics\n---\n\nbody");
      makeMe.entityPersister.merge(note);
      noteLevelIndexService.refreshForNote(note);

      assertThat(levelRows(note), empty());
    }

    @ParameterizedTest
    @MethodSource("invalidNoteLevelContents")
    void does_not_persist_invalid_note_level(String content) {
      Note note = noteWithContent(content);

      noteLevelIndexService.refreshForNote(note);

      assertThat(levelRows(note), empty());
    }

    static Stream<String> invalidNoteLevelContents() {
      return Stream.of(
          "---\nnote_level: 0\n---\n\nbody",
          "---\nnote_level: 7\n---\n\nbody",
          "---\nnote_level: 2.5\n---\n\nbody",
          "---\nnote_level: true\n---\n\nbody",
          "---\nnote_level:\n---\n\nbody",
          "---\nnote_level:\n  - 2\n---\n\nbody",
          "---\nnote_level:\n  nested: 2\n---\n\nbody",
          "---\nnote_level 2: 3\n---\n\nbody");
    }
  }
}
