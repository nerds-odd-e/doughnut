package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Comment;
import com.odde.doughnut.entities.Note;
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

import java.util.List;

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
    class NoteCommentsTest {
        @Test
        void shouldBeAbleToSeeNoteComments() {
            makeMe.aComment(note, userModel.getEntity()).please(true);
            makeMe.aComment(note, userModel.getEntity()).please(true);
            final List<Comment> comments = controller.noteComments(note);
            assertThat(comments.size(), equalTo(2));
        }

    }

    @Nested
    class AddCommentTest {

        @Test
        void shouldBeAbleToAddAComment() throws NoAccessRightException {
            String content = "My comment";

            Comment comment = controller.addComment(note, content);

            assertThat(comment.getContent(), equalTo(content));
            assertThat(comment.getId(), notNullValue());
            assertThat(comment.getNote(), equalTo(note));
            assertThat(comment.getUser(), equalTo(userModel.getEntity()));
            assertThat(comment.getCreatedAt(), notNullValue());
            assertThat(comment.getUpdatedAt(), notNullValue());
        }

    }

    @Nested
    class DeleteCommentTest {

        @Test
        void shouldBeAbleToDeleteAComment() throws NoAccessRightException {
            String content = "My comment";
            Comment comment = makeMe.aComment(note, userModel.getEntity(), content).please();

            Integer commentId = controller.deleteComment(comment);
            makeMe.refresh(comment);
            assertThat(comment.getDeletedAt(), notNullValue());
            assertThat(commentId, equalTo(comment.getId()));
        }

    }

    @Nested
    class EditCommentTest {

        @Test
        void shouldBeAbleToEditAComment() throws NoAccessRightException {
            String content = "My comment";
            Comment comment = makeMe.aComment(note, userModel.getEntity(), content).please();
            comment.setContent("edited");
            String response = controller.editComment(comment);

            makeMe.refresh(comment);
            assertThat(comment.getContent(), equalTo("edited"));
            assertThat(response, equalTo(comment.getContent()));
        }

    }
}