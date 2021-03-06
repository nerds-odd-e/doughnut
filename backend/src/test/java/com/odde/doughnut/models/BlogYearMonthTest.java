package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class BlogYearMonthTest {
  @Autowired MakeMe makeMe;

    @Test
    void shouldGetYearMonthEmptyList() {
        UserModel userModel = makeMe.aUser().toModelPlease();
        Note headNote = makeMe.aNote().byUser(userModel).please();
        BlogModel blogModel = new BlogModel(headNote.getNotebook());
        assertTrue(blogModel.getBlogYears(headNote).isEmpty());
    }

    @Test
    void shouldGetYearMonthList() {
        UserModel userModel = makeMe.aUser().toModelPlease();
        Note headNote = makeMe.aNote().byUser(userModel).please();
        BlogModel blogModel = new BlogModel(headNote.getNotebook());
        makeMe.aNote("2019/10/12: my post").under(headNote).please();
        makeMe.aNote("2019/11/12: my post2").under(headNote).please();
        makeMe.aNote("2020/11/12: my post3").under(headNote).please();
        makeMe.refresh(headNote);
        assertEquals(2, blogModel.getBlogYears(headNote).size());
    }

    @Test
    void shouldGetBlogPostList() {
        Note headNote = makeMe.aNote("odd-e-blog").withNoDescription().please();
        Note note1 = makeMe.aNote("1999/11/31: Article #1").description("Hello World").under(headNote).please();
        Note note2 = makeMe.aNote("1999/12/31: Article #2").description("Hello World").under(headNote).please();

        BlogModel blogModel = new BlogModel(headNote.getNotebook());
        makeMe.refresh(headNote);
        assertEquals(2, blogModel.getBlogPosts().size());
    }

}
