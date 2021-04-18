package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

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

    private Note topNote;
    private Note firstChild;
    private Timestamp firstChildUpdatetime;
    private String userName;

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        controller = new RestApiController(makeMe.modelFactoryService);

        topNote = makeMe.aNote("odd-e blog").please();
        firstChild = makeMe.aNote("how to do Scrum").description("Scrum").under(topNote).please();
        makeMe.refresh(topNote);
        makeMe.refresh(firstChild);
        firstChildUpdatetime = firstChild.getNoteContent().getUpdatedDatetime();

        userName = firstChild.getUser().getName();
    }

    @Test
    void noteApiResult() {
        Note.NoteApiResult note = controller.getNote();
        Note.NoteApiResult expected = new Note.NoteApiResult();
        expected.setTitle("how to do Scrum");
        expected.setDescription("Scrum");
        expected.setAuthor(userName);
        expected.setUpdateDatetime(firstChildUpdatetime.toString());

        assertThat(note.getTitle(), equalTo(expected.getTitle()));
        assertThat(note.getDescription(), equalTo(expected.getDescription()));
        assertThat(note.getAuthor(), equalTo(expected.getAuthor()));
        assertThat(note.getUpdateDatetime(), equalTo(expected.getUpdateDatetime()));
    }
}
