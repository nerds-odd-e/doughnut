package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ModelFactoryService;
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

    @Autowired
    ModelFactoryService modelFactoryService;
    private Note topNote;
    private Note firstChild;

    @BeforeEach
    void setup() throws NoAccessRightException, IOException {
        userModel = makeMe.aUser().toModelPlease();
        controller = new RestApiController(makeMe.modelFactoryService);

        topNote = makeMe.aNote("odd-e blog").please();
        firstChild = makeMe.aNote("how to do Scrum").description("Scrum").under(topNote).please();
        makeMe.refresh(topNote);
        makeMe.refresh(firstChild);
    }

    @Test
    void noteApiResult() {
        Note.NoteApiResult note = controller.getNote(userModel.getEntity());
        Note.NoteApiResult expected = new Note.NoteApiResult();
        expected.setTitle("how to do Scrum");
        expected.setDescription("Scrum");

        assertThat(note.getTitle(), equalTo(expected.getTitle()));
        assertThat(note.getDescription(), equalTo(expected.getDescription()));
    }
}
