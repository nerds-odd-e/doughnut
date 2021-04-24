package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.*;
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
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

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
    void getBlogArticlesByWebsiteName() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();

        int day = now.getDayOfMonth();
        String yearNoteTitle = String.valueOf(year);
        String monthNoteTitle = now.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        String dayNoteTitle = String.valueOf(day);


        Note headNote = makeMe.aNote("odd-e-blog").withNoDescription().please();
        Note yearNote = makeMe.aNote(yearNoteTitle).withNoDescription().under(headNote).please();
        Note monthNote = makeMe.aNote(monthNoteTitle).withNoDescription().under(yearNote).please();
        Note dayNote = makeMe.aNote(dayNoteTitle).withNoDescription().under(monthNote).please();
        Note note = makeMe.aNote("Hello World").description("Hello World").under(dayNote).please();
        makeMe.refresh(headNote);
        makeMe.refresh(yearNote);
        makeMe.refresh(monthNote);
        makeMe.refresh(dayNote);
        makeMe.refresh(note);

        List<BlogArticle> articles = controller.getBlogArticlesByWebsiteName(headNote.getTitle());

        assertThat(articles.size(), equalTo(1));
        BlogArticle article = articles.get(0);
        assertThat(article.getTitle(), equalTo(note.getTitle()));
        assertThat(article.getDescription(), equalTo(note.getNoteContent().getDescription()));
        assertThat(article.getAuthor(), equalTo(note.getUser().getName()));
        assertThat(article.getCreatedDatetime(), equalTo(note.getArticleDate()));
    }
}
