package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.models.BazaarModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

import javax.validation.Valid;
import java.io.IOException;

@Controller
@RequestMapping("/notebooks")
public class NotebookController extends ApplicationMvcController  {
    private final ModelFactoryService modelFactoryService;

    public NotebookController(CurrentUserFetcher currentUserFetcher, ModelFactoryService modelFactoryService) {
        super(currentUserFetcher);
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("")
    public String myNotebooks(Model model) {
        model.addAttribute("notebooks", getCurrentUser().getEntity().getOwnershipEntity().getNotebookEntities());
        model.addAttribute("subscriptions", getCurrentUser().getEntity().getSubscriptionEntities());
        return "notebooks/index";
    }

    @GetMapping({"/new"})
    public String newNote(Model model) {
        UserModel userModel = getCurrentUser();
        NoteContentEntity noteContentEntity = new NoteContentEntity();
        model.addAttribute("ownershipEntity", userModel.getEntity().getOwnershipEntity());
        model.addAttribute("noteContentEntity", noteContentEntity);
        return "notebooks/new";
    }

    @PostMapping({"/{ownershipEntity}/create"})
    public String createNote(@PathVariable(name = "ownershipEntity", required = false) OwnershipEntity ownershipEntity, @Valid NoteContentEntity noteContentEntity, BindingResult bindingResult) throws IOException {
        if (bindingResult.hasErrors()) {
            return "notebooks/new";
        }
        final Note note = new Note();
        UserEntity userEntity = getCurrentUser().getEntity();
        note.updateNoteContent(noteContentEntity, userEntity);
        note.buildNotebookEntityForHeadNote(ownershipEntity, userEntity);
        modelFactoryService.noteRepository.save(note);
        return "redirect:/notes/" + note.getId();
    }

    @GetMapping({"/{notebookEntity}/edit"})
    public String edit(@PathVariable(name = "notebookEntity") NotebookEntity notebookEntity) throws NoAccessRightException {
        UserModel userModel = getCurrentUser();
        userModel.assertAuthorization(notebookEntity);
        return "notebooks/edit";
    }

    @PostMapping(value = "/{notebookEntity}")
    @Transactional
    public String update(@Valid NotebookEntity notebookEntity, BindingResult bindingResult) throws NoAccessRightException {
        if (bindingResult.hasErrors()) {
            return "notebooks/edit";
        }
        getCurrentUser().assertAuthorization(notebookEntity);
        modelFactoryService.notebookRepository.save(notebookEntity);
        return "redirect:/notebooks";
    }

    @PostMapping(value = "/{notebookEntity}/share")
    public RedirectView shareNote(@PathVariable("notebookEntity") NotebookEntity notebookEntity) throws NoAccessRightException {
        getCurrentUser().assertAuthorization(notebookEntity);
        BazaarModel bazaar = modelFactoryService.toBazaarModel();
        bazaar.shareNote(notebookEntity);
        return new RedirectView("/notebooks");
    }

    private UserModel getCurrentUser() {
        return currentUserFetcher.getUser();
    }
}
