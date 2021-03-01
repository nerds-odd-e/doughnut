package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.controllers.exceptions.NoAccessRightException;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

@RestController
public class NoteRestController {
    private final CurrentUser currentUser;
    private final ModelFactoryService modelFactoryService;

    public NoteRestController(CurrentUser currentUser, ModelFactoryService modelFactoryService) {
        this.currentUser = currentUser;
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping(value = "/api/notes", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Note> getNotes() {
        return currentUser.getUser().getNotesInDescendingOrder();
    }

    @PostMapping(value = "/notes/{note}/delete")
    public RedirectView deleteNote(@PathVariable("note") Note note) throws NoAccessRightException {
        currentUser.getUser().checkAuthorization(note);
        modelFactoryService.noteRepository.delete(note);
        return new RedirectView("/notes");
    }

}
