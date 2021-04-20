package com.odde.doughnut.entities;

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
public class NoteBookTest {
    @Autowired MakeMe makeMe;
    @Test
    void notebookIsEmptyByDefault() {
        Note note = makeMe.aNote().please();
        Notebook notebook = note.getNotebook();
        makeMe.modelFactoryService.entityManager.createNativeQuery("UPDATE notebook SET notebook_type='GENERAL'").executeUpdate();
        makeMe.refresh(notebook);

    }
}
