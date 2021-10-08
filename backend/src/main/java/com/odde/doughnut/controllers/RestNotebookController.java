
package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.RedirectToNoteResponse;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.BazaarModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/notebooks")
class RestNotebookController {
    private final ModelFactoryService modelFactoryService;
    private final CurrentUserFetcher currentUserFetcher;
    @Resource(name = "testabilitySettings")
    private final TestabilitySettings testabilitySettings;

    public RestNotebookController(ModelFactoryService modelFactoryService, CurrentUserFetcher currentUserFetcher, TestabilitySettings testabilitySettings) {
        this.modelFactoryService = modelFactoryService;
        this.currentUserFetcher = currentUserFetcher;
        this.testabilitySettings = testabilitySettings;
    }

    static class NotebooksViewedByUser {
        public List<Notebook> notebooks;
        public List<Subscription> subscriptions;
    }

    @GetMapping("")
    public NotebooksViewedByUser myNotebooks() {
        UserModel user = currentUserFetcher.getUser();
        user.getAuthorization().assertLoggedIn();
        NotebooksViewedByUser notebooksViewedByUser = new NotebooksViewedByUser();
        notebooksViewedByUser.notebooks = user.getEntity().getOwnership().getNotebooks();
        notebooksViewedByUser.subscriptions = user.getEntity().getSubscriptions();
        return notebooksViewedByUser;
    }

    @PostMapping({"/create"})
    public RedirectToNoteResponse createNotebook(@Valid @ModelAttribute NoteContent noteContent) throws IOException {
        UserModel user = currentUserFetcher.getUser();
        user.getAuthorization().assertLoggedIn();
        User userEntity = user.getEntity();
        Note note = userEntity.getOwnership().createNotebook(userEntity, noteContent, testabilitySettings.getCurrentUTCTimestamp());
        modelFactoryService.noteRepository.save(note);
        return new RedirectToNoteResponse(note.getId());
    }

    @PostMapping(value = "/{notebook}")
    @Transactional
    public Notebook update(@Valid Notebook notebook) throws NoAccessRightException {
        UserModel user = currentUserFetcher.getUser();
        user.getAuthorization().assertAuthorization(notebook);
        modelFactoryService.notebookRepository.save(notebook);
        return notebook;
    }

    @PostMapping(value = "/{notebook}/share")
    public Notebook shareNote(@PathVariable("notebook") Notebook notebook) throws NoAccessRightException {
        UserModel user = currentUserFetcher.getUser();
        user.getAuthorization().assertAuthorization(notebook);
        BazaarModel bazaar = modelFactoryService.toBazaarModel();
        bazaar.shareNote(notebook);
        return notebook;
    }

    @PostMapping(value = "/{notebook}/copy")
    public RedirectToNoteResponse copyNotebook(@PathVariable("notebook") Notebook notebook) throws NoAccessRightException, IOException {
        UserModel user = currentUserFetcher.getUser();
        User userEntity = user.getEntity();
        user.getAuthorization().assertAuthorization(userEntity);
        Note note = userEntity.getOwnership().createNotebook(userEntity, notebook.getHeadNote().getNoteContent(), testabilitySettings.getCurrentUTCTimestamp());
        modelFactoryService.noteRepository.save(note);
        return new RedirectToNoteResponse(note.getId());


    }
}
