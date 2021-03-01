package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.DBCleaner;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
@Transactional
public class NoteRestControllerTests {
    @Autowired EntityManager entityManager;
    @Autowired ModelFactoryService modelFactoryService;
    private MakeMe makeMe;
    private UserEntity userEntity;
    private NoteRestController noteController;

    @BeforeEach
    void setup() {
        makeMe = new MakeMe();
        userEntity = makeMe.aUser().please(modelFactoryService);
        noteController = new NoteRestController(new TestCurrentUser(userEntity), modelFactoryService);
    }

    @Test
    void shouldGetListOfNotes() {
        NoteEntity note = makeMe.aNote().forUser(userEntity).please(modelFactoryService);
        makeMe.refresh(entityManager, userEntity);
        assertEquals(note.getTitle(), noteController.getNotes().get(0).getTitle());
    }


}
