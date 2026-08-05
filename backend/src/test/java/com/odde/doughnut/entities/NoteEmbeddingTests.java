package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class NoteEmbeddingTests {

  static List<List<Float>> floatRoundTripCases() {
    return List.of(List.of(1.0f, 2.5f, -3.7f, 0.0f, 100.123f), List.of(), List.of(42.0f));
  }

  @ParameterizedTest
  @MethodSource("floatRoundTripCases")
  void shouldConvertFloatsToBytesAndBack(List<Float> originalFloats) {
    NoteEmbedding noteEmbedding = new NoteEmbedding();
    noteEmbedding.setEmbeddingFromFloats(originalFloats);
    assertThat(noteEmbedding.getEmbeddingAsFloats(), equalTo(originalFloats));
  }

  @Test
  void shouldSetTimestampsOnCreate() {
    NoteEmbedding noteEmbedding = new NoteEmbedding();
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
    NoteEmbedding noteEmbedding = new NoteEmbedding();
    noteEmbedding.onCreate();
    Timestamp beforeUpdate = noteEmbedding.getUpdatedAt();

    noteEmbedding.onUpdate();

    assertThat(
        noteEmbedding.getUpdatedAt().after(beforeUpdate)
            || noteEmbedding.getUpdatedAt().equals(beforeUpdate),
        is(true));
  }
}
