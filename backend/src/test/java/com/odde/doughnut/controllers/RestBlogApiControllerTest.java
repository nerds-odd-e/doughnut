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

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class RestBlogApiControllerTest {
    @Autowired
    MakeMe makeMe;

    UserModel userModel;

    RestBlogApiController controller;

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        controller = new RestBlogApiController(makeMe.modelFactoryService);
    }

    @Test
    void getBlogArticlesByWebsiteName() {
        Note headNote = makeMe.aNote("odd-e-blog").withNoDescription().please();
        Note note = makeMe.aNote("1989/06/04: Hello World").description("Hello World").under(headNote).please();
        makeMe.refresh(headNote);

        List<BlogPost> articles = controller.getBlogPostsByWebsiteName();

        assertThat(articles.size(), equalTo(1));
        BlogPost article = articles.get(0);
        assertThat(article.getTitle(), equalTo("Hello World"));
        assertThat(article.getDescription(), equalTo(note.getNoteContent().getDescription()));
        assertThat(article.getAuthor(), equalTo(note.getUser().getName()));
    }
}
