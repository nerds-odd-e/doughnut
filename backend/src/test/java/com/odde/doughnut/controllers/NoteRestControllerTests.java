package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ModelFactoryService;
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
@Transactional
public class NoteRestControllerTests {
    @Autowired EntityManager entityManager;
    @Autowired ModelFactoryService modelFactoryService;
    @Autowired MakeMe makeMe;
    private UserModel userModel;
    private NoteRestController noteController;

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        noteController = new NoteRestController(new TestCurrentUserFetcher(userModel), modelFactoryService);
    }

    @Test
    void shouldGetListOfNotes() {
        NoteEntity note = makeMe.aNote().forUser(userModel).please();
        makeMe.refresh(entityManager, userModel.getEntity());
        assertEquals(note.getTitle(), noteController.getNotes().get(0).getTitle());
    }


}
