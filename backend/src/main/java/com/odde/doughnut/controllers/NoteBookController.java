package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;
import java.io.IOException;

@Controller
@RequestMapping("/notebooks")
public class NoteBookController extends ApplicationMvcController  {
    private final ModelFactoryService modelFactoryService;

    public NoteBookController(CurrentUserFetcher currentUserFetcher, ModelFactoryService modelFactoryService) {
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
        UserModel userModel = getCurrentUser();
        final NoteEntity noteEntity = new NoteEntity();
        final NotebookEntity notebookEntity = new NotebookEntity();
        UserEntity userEntity = userModel.getEntity();
        noteEntity.updateNoteContent(noteContentEntity, userEntity);
        noteEntity.setOwnershipEntity(ownershipEntity);
        noteEntity.setUserEntity(userEntity);
        noteEntity.setNotebookEntity(notebookEntity);

        notebookEntity.setCreatorEntity(userEntity);
        notebookEntity.setOwnershipEntity(ownershipEntity);
        modelFactoryService.noteRepository.save(noteEntity);

        notebookEntity.setHeadNoteEntity(noteEntity);
        modelFactoryService.notebookRepository.save(notebookEntity);
        return "redirect:/notes/" + noteEntity.getId();
    }

    private UserModel getCurrentUser() {
        return currentUserFetcher.getUser();
    }
}
