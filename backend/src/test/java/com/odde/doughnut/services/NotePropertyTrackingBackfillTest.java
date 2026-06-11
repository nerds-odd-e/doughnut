package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NotePropertyIndex;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.entities.repositories.NotePropertyIndexRepository;
import com.odde.doughnut.testability.MakeMe;
import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.Timestamp;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotePropertyTrackingBackfillTest {

  @Autowired MakeMe makeMe;
  @Autowired EntityManager entityManager;
  @Autowired DataSource dataSource;
  @Autowired NotePropertyIndexRepository notePropertyIndexRepository;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @Test
  void run_indexes_non_reserved_keys_and_seeds_skipped_trackers_for_owner() throws Exception {
    User owner = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
    Timestamp existingTrackerTime = makeMe.aTimestamp().of(1, 2).please();
    String markdown =
        "---\n"
            + "example of: \"[[Target]]\"\n"
            + "topic: physics\n"
            + "definition: term\n"
            + "image: /attachments/1\n"
            + "url: https://example.com\n"
            + "---\n\nbody";
    Note note = makeMe.aNote().notebook(notebook).content(markdown).please();
    MemoryTracker existingTopicTracker =
        makeMe
            .aMemoryTrackerFor(note)
            .by(owner)
            .propertyKey("topic")
            .assimilatedAt(existingTrackerTime)
            .please();

    Timestamp now = makeMe.aTimestamp().of(9, 9).please();
    runBackfill(now);

    assertThat(indexKeys(note), containsInAnyOrder("example of", "topic", "definition"));

    var trackers = memoryTrackerRepository.findByUserAndNote(owner.getId(), note.getId());
    assertThat(trackers, hasSize(2));

    MemoryTracker topicTracker =
        trackers.stream().filter(t -> "topic".equals(t.getPropertyKey())).findFirst().orElseThrow();
    assertThat(topicTracker.getId(), is(existingTopicTracker.getId()));
    assertThat(topicTracker.getAssimilatedAt(), is(existingTrackerTime));

    MemoryTracker definitionTracker =
        trackers.stream()
            .filter(t -> "definition".equals(t.getPropertyKey()))
            .findFirst()
            .orElseThrow();
    assertThat(definitionTracker.getRemovedFromTracking(), is(true));
    assertThat(definitionTracker.getAssimilatedAt(), is(now));
    assertThat(definitionTracker.getLastRecalledAt(), is(now));
    assertThat(definitionTracker.getNextRecallAt(), is(now));
    assertThat(definitionTracker.getRecallCount(), is(0));

    assertThat(trackers.stream().anyMatch(t -> "example of".equals(t.getPropertyKey())), is(false));
  }

  @Test
  void run_indexes_circle_owned_notes_without_seeding_trackers() throws Exception {
    User member = makeMe.aUser().please();
    Circle circle = makeMe.aCircle().hasMember(member).please();
    Notebook notebook = makeMe.aNotebook().owner(circle).please();
    Note note =
        makeMe
            .aNote()
            .notebook(notebook)
            .content("---\ntopic: shared\nexample of: \"[[X]]\"\n---\n")
            .please();

    runBackfill(makeMe.aTimestamp().of(3, 3).please());

    assertThat(indexKeys(note), containsInAnyOrder("topic", "example of"));
    assertThat(memoryTrackerRepository.findByUserAndNote(member.getId(), note.getId()), empty());
  }

  @Test
  void run_leaves_notes_without_frontmatter_unindexed() throws Exception {
    User owner = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
    Note note = makeMe.aNote().notebook(notebook).content("plain body").please();

    runBackfill(makeMe.aTimestamp().of(4, 4).please());

    assertThat(notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId()), empty());
    assertThat(memoryTrackerRepository.findByUserAndNote(owner.getId(), note.getId()), empty());
  }

  private void runBackfill(Timestamp now) throws Exception {
    entityManager.flush();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      NotePropertyTrackingBackfill.run(connection, now);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private java.util.List<String> indexKeys(Note note) {
    return notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId()).stream()
        .map(NotePropertyIndex::getPropertyKey)
        .toList();
  }
}
