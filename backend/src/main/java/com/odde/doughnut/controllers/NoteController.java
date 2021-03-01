package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.models.NoteModel;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import javax.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/notes")
public class NoteController {
    private final CurrentUser currentUser;
    private final ModelFactoryService modelFactoryService;

    public NoteController(CurrentUser currentUser, ModelFactoryService modelFactoryService) {
        this.currentUser = currentUser;
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("")
    public String myNotes(Model model) {
        model.addAttribute("notes", currentUser.getUser().getOrphanedNotes());
        return "my_notes";
    }

    @GetMapping({"/new", "/{parentNote}/new"})
    public String newNote(@PathVariable(name = "parentNote", required = false) Note parentNote, Model model) {
        Note note = new Note();
        note.setParentNote(parentNote);
        model.addAttribute("note", note);
        return "new_note";
    }

    @PostMapping("")
    public String createNote(@Valid Note note, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "new_note";
        }
        User user = currentUser.getUser();
        note.setUser(user);
        modelFactoryService.noteRepository.save(note);
        return "redirect:/notes/" + note.getId();
    }

    @GetMapping("/{note}")
    public String note(@PathVariable(name = "note") Note note, Model model) {
        model.addAttribute("note", note);
        model.addAttribute("noteDecorated", modelFactoryService.toModel(note));
        return "note";
    }

    @GetMapping("/{note}/edit")
    public String editNote(Note note, Model model) {
        model.addAttribute("note", note);
        return "edit_note";
    }

    @PostMapping("/{note}")
    public String updateNote(@Valid Note note, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
           return "edit_note";
        }
        modelFactoryService.noteRepository.save(note);
        return "redirect:/notes/" + note.getId();
    }

    @GetMapping("/{note}/link")
    public String link(
            @PathVariable("note") Note note,
            @RequestParam(required = false) String searchTerm,
            Model model
    ) {
        List<Note> linkableNotes = currentUser.getUser().filterLinkableNotes(note, searchTerm);
        model.addAttribute("linkableNotes", linkableNotes);
        model.addAttribute("sourceNote", note);
        return "link";
    }

    @PostMapping(value = "/{note}/link", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public RedirectView linkNote(@PathVariable("note") Note note, Integer targetNoteId) {
        Note targetNote = modelFactoryService.noteRepository.findById(targetNoteId).get();
        NoteModel noteModel = modelFactoryService.toModel(note);
        noteModel.linkNote(targetNote);
        return new RedirectView("/review");
    }

    @GetMapping("/{note}/move")
    public String moveNote(Note note, Model model) {
        model.addAttribute("note", note);
        return "move_note";
    }

}
