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
        model.addAttribute("notebooks", getCurrentUser().getEntity().getOwnership().getNotebooks());
        model.addAttribute("subscriptions", getCurrentUser().getEntity().getSubscriptions());
        return "notebooks/index";
    }

    @GetMapping({"/new"})
    public String newNote(Model model) {
        UserModel userModel = getCurrentUser();
        NoteContent noteContent = new NoteContent();
        model.addAttribute("ownership", userModel.getEntity().getOwnership());
        model.addAttribute("noteContent", noteContent);
        return "notebooks/new";
    }

    @PostMapping({"/{ownership}/create"})
    public String createNote(@PathVariable(name = "ownership", required = false) Ownership ownership, @Valid NoteContent noteContent, BindingResult bindingResult) throws IOException {
        if (bindingResult.hasErrors()) {
            return "notebooks/new";
        }
        final Note note = new Note();
        User user = getCurrentUser().getEntity();
        note.updateNoteContent(noteContent, user);
        note.buildNotebookForHeadNote(ownership, user);
        modelFactoryService.noteRepository.save(note);
        return "redirect:/notes/" + note.getId();
    }

    @GetMapping({"/{notebook}/edit"})
    public String edit(@PathVariable(name = "notebook") Notebook notebook) throws NoAccessRightException {
        UserModel userModel = getCurrentUser();
        userModel.assertAuthorization(notebook);
        return "notebooks/edit";
    }

    @PostMapping(value = "/{notebook}")
    @Transactional
    public String update(@Valid Notebook notebook, BindingResult bindingResult) throws NoAccessRightException {
        if (bindingResult.hasErrors()) {
            return "notebooks/edit";
        }
        getCurrentUser().assertAuthorization(notebook);
        modelFactoryService.notebookRepository.save(notebook);
        return "redirect:/notebooks";
    }

    @PostMapping(value = "/{notebook}/share")
    public RedirectView shareNote(@PathVariable("notebook") Notebook notebook) throws NoAccessRightException {
        getCurrentUser().assertAuthorization(notebook);
        BazaarModel bazaar = modelFactoryService.toBazaarModel();
        bazaar.shareNote(notebook);
        return new RedirectView("/notebooks");
    }

    private UserModel getCurrentUser() {
        return currentUserFetcher.getUser();
    }
}
