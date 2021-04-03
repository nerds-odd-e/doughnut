package com.odde.doughnut.entities;

import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoteContentEntityTest {
    MakeMe makeMe = new MakeMe();

    @Test
    void hasTitleInArticle() {
        NoteEntity note = makeMe.aNote().inMemoryPlease();
        assertTrue(note.hasTitleInArticle());
    }

    @Test
    void hasNoTitleInArticleBecauseOfNoDescription() {
        NoteEntity note = makeMe.aNote().withNoDescription().inMemoryPlease();
        assertFalse(note.hasTitleInArticle());
    }

    @Test
    void hasTitleInArticleBecauseOfChild() {
        NoteEntity note = makeMe.aNote().withNoDescription().inMemoryPlease();
        note.getChildren().add(makeMe.aNote().inMemoryPlease());
        assertTrue(note.hasTitleInArticle());
    }

}