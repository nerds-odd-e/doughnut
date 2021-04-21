package com.odde.doughnut.models;

import static org.junit.jupiter.api.Assertions.assertTrue;

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

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class BlogYearMonthTest {
  @Autowired MakeMe makeMe;

    @Autowired
    ModelFactoryService modelFactoryService;

    @Test
    void shouldGetYearMonthEmptyList() {
        Notebook notebook = makeMe.aNotebook().please();
        Note headNote = notebook.getHeadNote();
        BlogModel blogModel = new BlogModel(headNote, modelFactoryService);
        assertTrue(blogModel.getBlogYearMonths(headNote.getId()).isEmpty());
    }

    @Test
    void shouldGetYearMonthList() {
        Notebook notebook = makeMe.aNotebook().please();
        Note headNote = notebook.getHeadNote();
        BlogModel blogModel = new BlogModel(headNote, modelFactoryService);
        makeMe.aNote("2021").under(headNote).please();
        makeMe.aNote("2020").under(headNote).please();
        makeMe.aNote("2019").under(headNote).please();

        assertTrue(blogModel.getBlogYearMonths(headNote.getId()).size()==0);
    }
}
