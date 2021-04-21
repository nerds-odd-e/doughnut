package com.odde.doughnut.models;

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

import static org.junit.jupiter.api.Assertions.*;

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
        assertFalse(blogModel.getBlogYearMonths(headNote).isEmpty());
    }

    @Test
    void shouldGetYearMonthList() {
        Notebook notebook = makeMe.aNotebook().please();
        Note headNote = notebook.getHeadNote();
        BlogModel blogModel = new BlogModel(headNote, modelFactoryService);
        Note note1 = makeMe.aNote("2021").under(headNote).please();
        Note note2 = makeMe.aNote("2020").under(headNote).please();
        Note note3 = makeMe.aNote("2019").under(headNote).please();
        makeMe.refresh(notebook);
        makeMe.refresh(headNote);
        makeMe.refresh(note1);
        makeMe.refresh(note2);
        makeMe.refresh(note3);
        assertTrue(blogModel.getBlogYearMonths(headNote.getId()).size()==3);
    }



}
