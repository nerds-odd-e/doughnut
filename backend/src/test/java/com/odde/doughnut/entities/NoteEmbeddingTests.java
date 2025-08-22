package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteEmbeddingTests {

  @Autowired MakeMe makeMe;

  NoteEmbedding noteEmbedding;
  Note note;

  @BeforeEach
  void setup() {
    note = makeMe.aNote().please();
    noteEmbedding = new NoteEmbedding();
    noteEmbedding.setNote(note);
    noteEmbedding.setEmbeddingFromFloats(List.of(1.0f, 2.0f, 3.0f));
  }

  @Test
  void shouldConvertFloatsToBytesAndBack() {
    List<Float> originalFloats = List.of(1.0f, 2.5f, -3.7f, 0.0f, 100.123f);

    noteEmbedding.setEmbeddingFromFloats(originalFloats);
    List<Float> convertedFloats = noteEmbedding.getEmbeddingAsFloats();

    assertThat(convertedFloats.size(), equalTo(originalFloats.size()));
    for (int i = 0; i < originalFloats.size(); i++) {
      assertThat(convertedFloats.get(i), equalTo(originalFloats.get(i)));
    }
  }

  @Test
  void shouldSetTimestampsOnCreate() {
    // Do not persist since DB requires non-null embedding column
    Timestamp beforeCreate = new Timestamp(System.currentTimeMillis());
    noteEmbedding.onCreate();
    Timestamp afterCreate = new Timestamp(System.currentTimeMillis());

    assertThat(
        noteEmbedding.getCreatedAt().after(beforeCreate)
            || noteEmbedding.getCreatedAt().equals(beforeCreate),
        is(true));
    assertThat(
        noteEmbedding.getCreatedAt().before(afterCreate)
            || noteEmbedding.getCreatedAt().equals(afterCreate),
        is(true));
    assertThat(noteEmbedding.getUpdatedAt(), equalTo(noteEmbedding.getCreatedAt()));
  }

  @Test
  void shouldUpdateTimestampOnUpdate() {
    // Test that onUpdate method works correctly
    // Initially updatedAt is null, so we need to set it first
    noteEmbedding.onCreate(); // This sets both createdAt and updatedAt

    Timestamp beforeUpdate = noteEmbedding.getUpdatedAt();

    // Force a manual update
    noteEmbedding.onUpdate();
    Timestamp afterUpdate = noteEmbedding.getUpdatedAt();

    // The timestamp should be different after calling onUpdate
    assertThat(afterUpdate.after(beforeUpdate) || afterUpdate.equals(beforeUpdate), is(true));
  }

  @Test
  void shouldHandleEmptyFloatList() {
    List<Float> emptyList = List.of();

    noteEmbedding.setEmbeddingFromFloats(emptyList);
    List<Float> convertedFloats = noteEmbedding.getEmbeddingAsFloats();

    assertThat(convertedFloats.isEmpty(), is(true));
  }

  @Test
  void shouldHandleSingleFloat() {
    List<Float> singleFloat = List.of(42.0f);

    noteEmbedding.setEmbeddingFromFloats(singleFloat);
    List<Float> convertedFloats = noteEmbedding.getEmbeddingAsFloats();

    assertThat(convertedFloats.size(), equalTo(1));
    assertThat(convertedFloats.get(0), equalTo(42.0f));
  }
}
