package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.DecoratorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.util.List;

@Controller
public class NoteController {
    private final CurrentUser currentUser;
    private final NoteRepository noteRepository;
    private final DecoratorService decoratorService;

    public NoteController(CurrentUser currentUser, NoteRepository noteRepository, DecoratorService decoratorService) {
        this.currentUser = currentUser;
        this.noteRepository = noteRepository;
        this.decoratorService = decoratorService;
    }

    @GetMapping({"/notes/new", "/notes/{parent_id}/new"})
    public String newNote(@PathVariable(name = "parent_id", required = false) Integer parentId, Model model) {
        Note note = new Note();
        if (parentId != null) {
            Note parentNote = noteRepository.findById(parentId).get();
            note.setParentNote(parentNote);
        }
        model.addAttribute("note", note);
        return "new_note";
    }

    @GetMapping("/notes")
    public String myNotes(Model model) {
        model.addAttribute("notes", currentUser.getUser().getOrphanedNotes());
        return "my_notes";
    }

    @PostMapping("/notes")
    public String createNote(@Valid Note note, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "new_note";
        }
        User user = currentUser.getUser();
        note.setUser(user);
        noteRepository.save(note);
        return "redirect:/notes/" + note.getId();
    }

    @GetMapping("/notes/{note}/edit")
    public String editNote(Note note, Model model) {
        model.addAttribute("note", note);
        return "edit_note";
    }

    @PostMapping("/notes/{note}")
    public String updateNote(@Valid Note note, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
           return "edit_note";
        }
        noteRepository.save(note);
        return "redirect:/notes/" + note.getId();
    }

    @GetMapping("/notes/{id}")
    public String note(@PathVariable(name = "id") Integer noteId, Model model) {
        Note note = noteRepository.findById(noteId).get();
        model.addAttribute("note", note);
        model.addAttribute("noteDecorated", decoratorService.decorate(note));
        return "note";
    }

    @GetMapping("/notes/{id}/link")
    public String link(
            @PathVariable("id") String id,
            @RequestParam(required = false) String searchTerm,
            Model model
    ) {
        Note sourceNote = noteRepository.findById(Integer.valueOf(id)).get();
        List<Note> linkableNotes = currentUser.getUser().filterLinkableNotes(sourceNote, searchTerm);
        model.addAttribute("linkableNotes", linkableNotes);
        model.addAttribute("sourceNote", sourceNote);
        return "link";
    }

    @GetMapping("/notes/{note}/move")
    public String moveNote(Note note, Model model) {
        model.addAttribute("note", note);
        return "move_note";
    }

}
