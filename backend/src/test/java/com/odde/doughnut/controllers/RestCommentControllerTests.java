package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Comment;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.NotesBulk;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class RestCommentControllerTests {
    @Autowired
    ModelFactoryService modelFactoryService;

    @Autowired
    MakeMe makeMe;
    private UserModel userModel;
    private final TestabilitySettings testabilitySettings = new TestabilitySettings();
    private Note note;
    RestCommentController controller;

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        controller = new RestCommentController(modelFactoryService, new TestCurrentUserFetcher(userModel), testabilitySettings);
        note = makeMe.aNote().byUser(userModel).please();
    }

    @Nested
    class createCommentTest {
        void shouldBeAbleToSaveCommentWhenValid() throws NoAccessRightException {
            Note note = null;
            Comment newComment = null;
            controller.create(note, newComment);

            makeMe.refresh(newComment);
            assertThat(newComment.getId(), notNullValue());
        }
    }

    @Nested
    class DeleteCommentTest {

        @Test
        void shouldBeAbleToDeleteAComment() throws NoAccessRightException {
            String content = "My comment";
            Comment comment = makeMe.aComment(note, userModel.getEntity(), content).please();

            controller.deleteComment(comment);
            makeMe.refresh(comment);
            assertThat(comment.getDeletedAt(), notNullValue());
        }

    }
}