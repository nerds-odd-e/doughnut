package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
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
import java.util.List;

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
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        controller = new RestApiController(makeMe.modelFactoryService);
    }

    @Test
    void noteApiResult() {
        Note topNote = makeMe.aNote("odd-e blog").please();
        Note firstChild = makeMe.aNote("how to do Scrum").description("Scrum").under(topNote).please();
        makeMe.refresh(topNote);
        makeMe.refresh(firstChild);
        Timestamp firstChildUpdatetime = firstChild.getNoteContent().getUpdatedDatetime();
        String userName = firstChild.getUser().getName();

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

    @Test
    void getBlogArticlesByNotebookId() {
        Note headNote = makeMe.aNote("odd-e blog").please();
        Note note = makeMe.aNote("Hello World").under(headNote).please();
        Notebook notebook = headNote.getNotebook();
        makeMe.refresh(notebook);
        makeMe.refresh(headNote);
        makeMe.refresh(note);

        List<Note> articles = controller.getBlogArticlesByNotebookId(headNote.getNotebook().getId());

        assertThat(articles.size(), equalTo(1));
        assertThat(articles.get(0).getArticleTitle(), equalTo(note.getArticleTitle()));
    }
}
