package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.sql.Date;
import java.time.LocalDate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class UserModelTest {
    @Autowired EntityManager entityManager;

    @Autowired MakeMe makeMe;

    @Test
    void shouldReturnEmptyListWhenThhereIsNoNode() {
        UserModel userModel = makeMe.aUser().toModelPlease();
        assertEquals(0, userModel.getNewNotesToReview().size());
    }

    @Test
    void shouldReturnTheNoteWhenThereIsOne() {
        UserModel userModel = makeMe.aUser().toModelPlease();
        NoteEntity note = makeMe.aNote().forUser(userModel).please();
        makeMe.refresh(entityManager, userModel.getEntity());
        assertThat(userModel.getNewNotesToReview(), contains(note));
    }

    @Test
    void shouldReturnTheNoteWhenThereIsTwo() {
        UserModel userModel = makeMe.aUser().toModelPlease();
        Date yesterday = Date.valueOf(LocalDate.now().minusDays(1));
        NoteEntity note1 = makeMe.aNote().forUser(userModel).updatedAt(yesterday).please();
        NoteEntity note2 = makeMe.aNote().forUser(userModel).please();
        makeMe.refresh(entityManager, userModel.getEntity());

        assertEquals(note2.getTitle(), userModel.getNewNotesToReview().get(0).getTitle());
    }

}