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
@RequestMapping("/notes")
public class NoteBookController extends ApplicationMvcController  {
    private final ModelFactoryService modelFactoryService;

    public NoteBookController(CurrentUserFetcher currentUserFetcher, ModelFactoryService modelFactoryService) {
        super(currentUserFetcher);
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping({"/new_notebook"})
    public String newNote(Model model) {
        UserModel userModel = getCurrentUser();
        NoteContentEntity noteContentEntity = new NoteContentEntity();
        model.addAttribute("ownershipEntity", userModel.getEntity().getOwnershipEntity());
        model.addAttribute("noteContentEntity", noteContentEntity);
        return "notes/new_notebook";
    }

    @PostMapping({"/{ownershipEntity}/create_notebook"})
    public String createNote(@PathVariable(name = "ownershipEntity", required = false) OwnershipEntity ownershipEntity, @Valid NoteContentEntity noteContentEntity, BindingResult bindingResult) throws IOException {
        if (bindingResult.hasErrors()) {
            return "notes/new_notebook";
        }
        UserModel userModel = getCurrentUser();
        final NoteEntity noteEntity = new NoteEntity();
        noteEntity.populate(ownershipEntity, null, noteContentEntity, userModel.getEntity());
        final NotebookEntity notebookEntity = new NotebookEntity();
        noteEntity.setNotebookEntity(notebookEntity);
        notebookEntity.setCreatorEntity(getCurrentUser().getEntity());
        notebookEntity.setOwnershipEntity(getCurrentUser().getOwnershipModel().getEntity());
        modelFactoryService.noteRepository.save(noteEntity);
        notebookEntity.setHeadNoteEntity(noteEntity);
        modelFactoryService.notebookRepository.save(notebookEntity);
        return "redirect:/notes/" + noteEntity.getId();
    }

    private UserModel getCurrentUser() {
        return currentUserFetcher.getUser();
    }
}
