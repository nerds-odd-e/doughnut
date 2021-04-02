package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.SubscriptionEntity;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.view.RedirectView;

import javax.validation.Valid;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class BazaarForUserControllerTest {
    @Autowired
    private MakeMe makeMe;
    private UserModel userModel;
    private NoteEntity topNote;
    private BazaarForUserController controller;
    final ExtendedModelMap model = new ExtendedModelMap();


    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        topNote = makeMe.aNote().byUser(userModel).please();
        makeMe.aBazaarNode(topNote).please();
        controller = new BazaarForUserController(new TestCurrentUserFetcher(userModel), makeMe.modelFactoryService);
    }

    @Test
    void subscribeToNoteSuccessfully() throws NoAccessRightException {
        SubscriptionEntity subscriptionEntity = makeMe.aSubscriptionFor().inMemoryPlease();
        String result = controller.createSubscription(
                topNote,
                subscriptionEntity,
                makeMe.successfulBindingResult(), model);
        assertThat(result, matchesPattern("redirect:/subscriptions/\\d+"));
        assertEquals(topNote, subscriptionEntity.getNoteEntity());
        assertEquals(userModel.getEntity(), subscriptionEntity.getUserEntity());
    }

    @Test
    void shouldShowTheFormAgainIfError() throws NoAccessRightException {
        SubscriptionEntity subscriptionEntity = makeMe.aSubscriptionFor().inMemoryPlease();
        String result = controller.createSubscription(
                topNote,
                subscriptionEntity,
                makeMe.failedBindingResult(), model);
        assertEquals("subscriptions/add_to_learning", result);
    }

    @Test
    void notAllowToSubscribeToNoneBazaarNote() {
        NoteEntity anotherNote = makeMe.aNote().byUser(userModel).please();
        SubscriptionEntity subscriptionEntity = makeMe.aSubscriptionFor().inMemoryPlease();
        assertThrows(NoAccessRightException.class, ()-> controller.createSubscription(
                anotherNote,
                subscriptionEntity,
                makeMe.successfulBindingResult(), model));
    }
}
