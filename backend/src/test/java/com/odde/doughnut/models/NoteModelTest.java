package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteMotion;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class NoteModelTest {
    @Autowired
    MakeMe makeMe;
    Note subject;
    Note parent;
    Note child;
    UserModel userModel;
    NoteModel model;
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        parent = makeMe.aNote().byUser(userModel).please();
        subject = makeMe.aNote().under(parent).byUser(userModel).please();
        child = makeMe.aNote("child").under(subject).byUser(userModel).please();
        makeMe.refresh(subject);
        model = makeMe.modelFactoryService.toNoteModel(subject);
    }

    @Test
    void deleteNoteShouldAlsoDeleteTheLinks() {
        Note target = makeMe.aNote("target").byUser(userModel).please();
        makeMe.aLink().between(subject, target).please();
        model.destroy(timestamp);
        makeMe.refresh(target);
        assertThat(target.getRefers(), hasSize(0));
    }
}

