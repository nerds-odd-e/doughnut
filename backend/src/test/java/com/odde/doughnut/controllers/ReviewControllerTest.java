package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewSetting;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
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

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class ReviewControllerTest {
    @Autowired
    ModelFactoryService modelFactoryService;
    @Autowired
    MakeMe makeMe;
    private TestabilitySettings testabilitySettings = new TestabilitySettings();
    private UserModel userModel;
    private Note parentNote;
    final ExtendedModelMap model = new ExtendedModelMap();
    ReviewController controller;

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        controller = new ReviewController(new TestCurrentUserFetcher(userModel), modelFactoryService, testabilitySettings);
    }

    @Test
    void skip() {
        ReviewPoint rp = makeMe.aReviewPointFor(makeMe.aNote().please()).inMemoryPlease();
        controller.skip(rp, new ReviewSetting());
        assertThat(rp.getId(), is(not(nullValue())));
        assertThat(rp.getRemovedFromReview(), is(true));
    }

}