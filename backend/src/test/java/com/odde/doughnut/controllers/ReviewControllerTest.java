package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.entities.ReviewSettingEntity;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TimeTraveler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class ReviewControllerTest {
    @Autowired ModelFactoryService modelFactoryService;
    @Autowired MakeMe makeMe;
    private TimeTraveler timeTraveler = new TimeTraveler();
    private UserModel userModel;
    private NoteEntity parentNote;
    final ExtendedModelMap model = new ExtendedModelMap();
    ReviewController controller;

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        controller = new ReviewController(new TestCurrentUserFetcher(userModel), modelFactoryService, timeTraveler);
    }

    @Test
    void skip() {

        ReviewPointEntity rp = makeMe.aReviewPointFor(makeMe.aNote().please()).inMemoryPlease();
        controller.skip(rp, new ReviewSettingEntity());
        assertThat(rp.getId(), is(not(nullValue())));
        assertThat(rp.getRemovedFromReview(), is(true));
    }

}