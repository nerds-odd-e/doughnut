package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class SubscriptionControllerTest {
    @Autowired
    private MakeMe makeMe;
    private UserModel userModel;
    private Note topNote;
    private Notebook notebook;
    private SubscriptionController controller;
    final ExtendedModelMap model = new ExtendedModelMap();


    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        topNote = makeMe.aNote().byUser(userModel).please();
        notebook = topNote.getNotebook();
        makeMe.aBazaarNodebook(topNote.getNotebook()).please();
        controller = new SubscriptionController(new TestCurrentUserFetcher(userModel), makeMe.modelFactoryService);
    }

    @Test
    void subscribeToNoteSuccessfully() throws NoAccessRightException {
        Subscription subscription = makeMe.aSubscription().inMemoryPlease();
        String result = controller.createSubscription(
                notebook,
                subscription,
                makeMe.successfulBindingResult(), model);
        assertThat(result, matchesPattern("redirect:/subscriptions/\\d+"));
        assertEquals(topNote, subscription.getHeadNote());
        assertEquals(userModel.getEntity(), subscription.getUserEntity());
    }

    @Test
    void shouldShowTheFormAgainIfError() throws NoAccessRightException {
        Subscription subscription = makeMe.aSubscription().inMemoryPlease();
        String result = controller.createSubscription(
                notebook,
                subscription,
                makeMe.failedBindingResult(), model);
        assertEquals("subscriptions/add_to_learning", result);
    }

    @Test
    void notAllowToSubscribeToNoneBazaarNote() {
        Note anotherNote = makeMe.aNote().byUser(userModel).please();
        Subscription subscription = makeMe.aSubscription().inMemoryPlease();
        assertThrows(NoAccessRightException.class, ()-> controller.createSubscription(
                anotherNote.getNotebook(),
                subscription,
                makeMe.successfulBindingResult(), model));
    }
}
