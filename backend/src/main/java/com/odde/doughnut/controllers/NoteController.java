package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.controllers.exceptions.NoAccessRightException;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.models.NoteModel;
import com.odde.doughnut.models.NoteMotion;
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
    public String newNote(@PathVariable(name = "parentNote", required = false) NoteEntity parentNote, Model model) {
        NoteEntity noteEntity = new NoteEntity();
        noteEntity.setParentNote(parentNote);
        model.addAttribute("noteEntity", noteEntity);
        return "new_note";
    }

    @PostMapping("")
    public String createNote(@Valid NoteEntity noteEntity, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "new_note";
        }
        UserEntity userEntity = currentUser.getUser();
        noteEntity.setUserEntity(userEntity);
        modelFactoryService.noteRepository.save(noteEntity);
        return "redirect:/notes/" + noteEntity.getId();
    }

    @GetMapping("/{noteEntity}")
    public String showNote(@PathVariable(name = "noteEntity") NoteEntity noteEntity, Model model) {
        model.addAttribute("noteDecorated", modelFactoryService.toModel(noteEntity));
        return "show_note";
    }

    @GetMapping("/{noteEntity}/edit")
    public String editNote(NoteEntity noteEntity) {
        return "edit_note";
    }

    @PostMapping("/{noteEntity}")
    public String updateNote(@Valid NoteEntity noteEntity, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "edit_note";
        }
        modelFactoryService.noteRepository.save(noteEntity);
        return "redirect:/notes/" + noteEntity.getId();
    }

    @GetMapping("/{noteEntity}/link")
    public String link( @PathVariable("noteEntity") NoteEntity noteEntity, @RequestParam(required = false) String searchTerm, Model model) {
        List<NoteEntity> linkableNotes = currentUser.getUser().filterLinkableNotes(noteEntity, searchTerm);
        model.addAttribute("linkableNotes", linkableNotes);
        return "link";
    }

    @PostMapping(value = "/{noteEntity}/link", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public RedirectView linkNote(@PathVariable("noteEntity") NoteEntity noteEntity, Integer targetNoteId) {
        NoteEntity targetNote = modelFactoryService.noteRepository.findById(targetNoteId).get();
        NoteModel noteModel = modelFactoryService.toModel(noteEntity);
        noteModel.linkNote(targetNote);
        return new RedirectView("/review");
    }

    @GetMapping("/{noteEntity}/move")
    public String prepareToNote(NoteEntity noteEntity, Model model) {
        model.addAttribute("noteMotion", this.modelFactoryService.getLeftNoteMotion(noteEntity));
        model.addAttribute("noteMotionRight", this.modelFactoryService.getRightNoteMotion(noteEntity));
        model.addAttribute("noteMotionUnder", new NoteMotion(null, true));
        return "move_note";
    }

    @PostMapping("/{noteEntity}/move")
    public String moveNote(NoteEntity noteEntity, NoteMotion noteMotion, Model model) {
        noteMotion.execute(noteEntity, this.modelFactoryService);
        return "redirect:/notes/" + noteEntity.getId();
    }

    @PostMapping(value = "/{noteEntity}/delete")
    public RedirectView deleteNote(@PathVariable("noteEntity") NoteEntity noteEntity) throws NoAccessRightException {
        currentUser.getUser().checkAuthorization(noteEntity);
        modelFactoryService.noteRepository.delete(noteEntity);
        return new RedirectView("/notes");
    }

}
