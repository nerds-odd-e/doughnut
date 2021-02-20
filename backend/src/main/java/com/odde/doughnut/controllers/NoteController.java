package com.odde.doughnut.controllers;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.repositories.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class NoteController {
    private final UserRepository userRepository;
    private final NoteRepository noteRepository;

    public NoteController(UserRepository userRepository, NoteRepository noteRepository) {
        this.userRepository = userRepository;
        this.noteRepository = noteRepository;
    }

    @GetMapping("/note")
    public String notes(Model model) {
        model.addAttribute("note", new Note());
        return "note";
    }

    @GetMapping("/all_my_notes")
    public String all_my_notes(@RequestAttribute("currentUser") User currentUser, Model model) {
        model.addAttribute("all_my_notes", currentUser.getNotes());
        return "all_my_notes";
    }

    @GetMapping("/link/{id}")
    public String link(@RequestAttribute("currentUser") User currentUser, Model model, @PathVariable("id") String id, @RequestParam(required = false) String searchTerm) {
        Optional<Note> sourceNote = noteRepository.findById(Integer.valueOf(id));
        List<Note> linkableNotes = getLinkableNotes(currentUser, sourceNote);
        if(searchTerm != null) {
            linkableNotes = getFilteredLinkableNotes(linkableNotes, searchTerm);
        }
        model.addAttribute("linkableNotes", linkableNotes);
        model.addAttribute("sourceNote", sourceNote.get());
        return "link";
    }

    private List<Note> getLinkableNotes(User user, Optional<Note> sourceNote) {
        List<Note> targetNotes = sourceNote.get().getTargetNotes();
        List<Note> allNotes = user.getNotes();
        List<Note> linkableNotes = allNotes.stream()
                .filter(i -> !targetNotes.contains(i))
                .filter(i -> i.getId() != sourceNote.get().getId())
                .collect(Collectors.toList());
        return linkableNotes;
    }

    private List<Note> getFilteredLinkableNotes(List<Note> notes, String searchTerm ) {
        List<Note> filteredNotes = notes.stream()
                .filter(note -> note.getTitle().contains(searchTerm))
                .collect(Collectors.toList());
        return filteredNotes;
    }

}
