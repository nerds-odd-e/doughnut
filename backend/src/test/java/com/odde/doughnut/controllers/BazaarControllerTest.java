package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class BazaarControllerTest {
    @Autowired
    MakeMe makeMe;
    BazaarController controller;

    @BeforeEach
    void setup() {
        controller = new BazaarController(null, makeMe.modelFactoryService);
    }

    @Test
    void itShouldNotAllowVisitingAnyNote() {
        Note note = makeMe.aNote().please();
        assertThrows(NoAccessRightException.class, ()-> controller.showBazaarNote(note));
    }

    @Test
    void itShouldAllowVisitingBazaarNote() throws NoAccessRightException {
        Note note = makeMe.aNote().please();
        makeMe.aBazaarNodebook(note.getNotebookEntity()).please();
        assertThat(controller.showBazaarNote(note), equalTo("bazaar/show"));
    }

}