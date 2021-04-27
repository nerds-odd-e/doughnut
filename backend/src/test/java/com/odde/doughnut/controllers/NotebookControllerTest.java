package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.models.UserModel;
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
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class NotebookControllerTest {
    @Autowired
    ModelFactoryService modelFactoryService;
    @Autowired
    private MakeMe makeMe;
    private UserModel userModel;
    private Note topNote;
    private NotebookController controller;
    final ExtendedModelMap model = new ExtendedModelMap();


    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        topNote = makeMe.aNote().byUser(userModel).please();
        controller = new NotebookController(new TestCurrentUserFetcher(userModel), modelFactoryService);
    }

    @Nested
    class ShareMyNotebook {

        @Test
        void shareMyNote() throws NoAccessRightException {
            long oldCount = modelFactoryService.bazaarNotebookRepository.count();
            RedirectView rv = controller.shareNote(topNote.getNotebook());
            assertEquals("/notebooks", rv.getUrl());
            assertThat(modelFactoryService.bazaarNotebookRepository.count(), equalTo(oldCount + 1));
        }

        @Test
        void shouldNotBeAbleToShareNoteThatBelongsToOtherUser() {
            User anotherUser = makeMe.aUser().please();
            Note note = makeMe.aNote().byUser(anotherUser).please();
            assertThrows(NoAccessRightException.class, () ->
                    controller.shareNote(note.getNotebook())
            );
        }

    }

    @Nested
    class updateNotebook {
        @Test
        void shouldNotBeAbleToUpdateNotebookThatBelongsToOtherUser() {
            User anotherUser = makeMe.aUser().please();
            Note note = makeMe.aNote().byUser(anotherUser).please();
            assertThrows(NoAccessRightException.class, () ->
                    controller.update(note.getNotebook(), makeMe.successfulBindingResult())
            );
        }
    }

    @Nested
    class createBlog {

        @Test
        void shouldCreateNotebookAsABlogWhenBlogTypeIsSelected() throws IOException {
            User user = makeMe.aUser().please();
            Note note = makeMe.aNote().byUser(user).inMemoryPlease();

            NoteContent noteContent = note.getNoteContent();
            BindingResult bindingResult = makeMe.successfulBindingResult();
            String response = controller.createBlog(user.getOwnership(), noteContent, bindingResult);

            assertThat(response, matchesPattern("redirect:/notes/\\d+"));
            assertEquals(NotebookType.BLOG, getNotebookJustCreated(response).getNotebookType());
        }

        @Test
        void shouldUseTheRightTemplate() throws IOException {
            String response = controller.createBlog(null, null, makeMe.failedBindingResult());

            assertThat(response, equalTo("notebooks/new_blog"));
        }

    }

    private Notebook getNotebookJustCreated(String response) {
        String[] split = response.split("/");
        Integer id = Integer.valueOf(split[split.length - 1]);
        Note createdNote = modelFactoryService.findNoteById(id).get();
        return createdNote.getNotebook();
    }
}
