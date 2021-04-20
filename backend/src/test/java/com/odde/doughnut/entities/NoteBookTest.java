package com.odde.doughnut.entities;

import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class NoteBookTest {

    @Autowired
    MakeMe makeMe;

    @Test
    void verifyNotebookTypeVarcharMappingToEnumForGeneral() {
        Note note = makeMe.aNote().please();
        Notebook notebook = note.getNotebook();
        makeMe.modelFactoryService.entityManager.createNativeQuery("UPDATE notebook SET notebook_type='GENERAL'").executeUpdate();
        makeMe.refresh(notebook);
        assertThat(notebook.getNotebookType(), equalTo(NotebookType.GENERAL));
    }

    @Test
    void verifyNotebookTypeVarcharMappingToEnumForBlog() {
        Note note = makeMe.aNote().please();
        Notebook notebook = note.getNotebook();
        makeMe.modelFactoryService.entityManager.createNativeQuery("UPDATE notebook SET notebook_type='BLOG'").executeUpdate();
        makeMe.refresh(notebook);
        assertThat(notebook.getNotebookType(), equalTo(NotebookType.BLOG));
    }
}
