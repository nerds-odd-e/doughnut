package com.odde.doughnut.models;

import com.odde.doughnut.entities.BlogYearMonth;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class BlogYearMonthTest {
  @Autowired MakeMe makeMe;

    @Autowired
    ModelFactoryService modelFactoryService;

    @Test
    void shouldGetYearMonthEmptyList() {
        UserModel userModel = makeMe.aUser().toModelPlease();
        Notebook notebook = makeMe.aNotebook().byUser(userModel).please();
        Note headNote = makeMe.aNote().byUser(userModel).underNotebook(notebook).please();
        BlogModel blogModel = new BlogModel(headNote, modelFactoryService);
        assertTrue(blogModel.getBlogYearMonths(headNote).isEmpty());
    }

    @Test
    void shouldGetYearMonthList() {
        UserModel userModel = makeMe.aUser().toModelPlease();
        Notebook notebook = makeMe.aNotebook().byUser(userModel).please();
        Note headNote = makeMe.aNote().byUser(userModel).underNotebook(notebook).please();
        BlogModel blogModel = new BlogModel(headNote, modelFactoryService);
        Note note1 = makeMe.aNote("2021").under(headNote).please();
        Note note2 = makeMe.aNote("2020").under(headNote).please();
        Note note3 = makeMe.aNote("2019").under(headNote).please();
        makeMe.refresh(notebook);
        makeMe.refresh(headNote);
        makeMe.refresh(note1);
        makeMe.refresh(note2);
        makeMe.refresh(note3);
        assertEquals(3, blogModel.getBlogYearMonths(headNote.getId()).size());
    }

    @Test
    void shouldGetBlogPostList() {
        Note headNote = makeMe.aNote("odd-e-blog").withNoDescription().please();
        Note note1 = makeMe.aNote("1999/11/31: Article #1").description("Hello World").under(headNote).please();
        Note note2 = makeMe.aNote("1999/12/31: Article #2").description("Hello World").under(headNote).please();

        BlogModel blogModel = new BlogModel(headNote, modelFactoryService);
        makeMe.refresh(headNote);
        assertEquals(2, blogModel.getBlogPosts(headNote).size());
    }

}
