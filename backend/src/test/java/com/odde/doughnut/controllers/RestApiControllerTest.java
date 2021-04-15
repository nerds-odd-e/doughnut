package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class RestApiControllerTest {
    @Autowired
    MakeMe makeMe;

    UserModel userModel;

    RestApiController controller;

    @BeforeEach
    void setup() throws NoAccessRightException, IOException {
        userModel = makeMe.aUser().toModelPlease();
        controller = new RestApiController(makeMe.modelFactoryService);
        final ExtendedModelMap model = new ExtendedModelMap();

        NoteController noteController = new NoteController(new TestCurrentUserFetcher(userModel), makeMe.modelFactoryService);

        Note parent = makeMe.aNote().byUser(userModel).please();
        Note newNote = makeMe.aNote().inMemoryPlease();
        BindingResult bindingResult = makeMe.successfulBindingResult();

        newNote.getNoteContent().setTitle("odd-e blog");
        newNote.getNoteContent().setDescription("test");

        noteController.createNote(parent, newNote.getNoteContent(), bindingResult, model);
    }

    @Test
    void noteApiResult() {
        Note.NoteApiResult note = controller.getNote(userModel.getEntity());
        Note.NoteApiResult expected = new Note.NoteApiResult();
        expected.setTitle("odd-e blog");
        expected.setDescription("test");

        assertThat(note.getTitle(), equalTo(expected.getTitle()));
        assertThat(note.getDescription(), equalTo(expected.getDescription()));
    }
}
