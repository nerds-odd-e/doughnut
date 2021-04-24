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
        Note headNote = makeMe.aNote("odd-e-blog").withNoDescription().please();
        Note note = makeMe.aNote("1989/06/04: Hello World").description("Hello World").under(headNote).please();
        makeMe.refresh(headNote);

        List<BlogArticle> articles = controller.getBlogPostsByWebsiteName(headNote.getTitle());

        assertThat(articles.size(), equalTo(1));
        BlogArticle article = articles.get(0);
        assertThat(article.getTitle(), equalTo("Hello World"));
        assertThat(article.getDescription(), equalTo(note.getNoteContent().getDescription()));
        assertThat(article.getAuthor(), equalTo(note.getUser().getName()));
    }
}
