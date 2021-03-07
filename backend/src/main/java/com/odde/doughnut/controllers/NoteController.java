package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.NoteMotionEntity;
import com.odde.doughnut.models.NoteContentModel;
import com.odde.doughnut.models.TreeNodeModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import javax.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/notes")
public class NoteController {
    private final CurrentUserFetcher currentUserFetcher;
    private final ModelFactoryService modelFactoryService;

    public NoteController(CurrentUserFetcher currentUserFetcher, ModelFactoryService modelFactoryService) {
        this.currentUserFetcher = currentUserFetcher;
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("")
    public String myNotes(Model model) {
        model.addAttribute("notes", currentUserFetcher.getUser().getOrphanedNotes());
        return "notes/my_notes";
    }

    @GetMapping({"/new", "/{parentNote}/new"})
    public String newNote(@PathVariable(name = "parentNote", required = false) NoteEntity parentNote, Model model) throws NoAccessRightException {
        if (parentNote != null) {
            currentUserFetcher.getUser().assertAuthorization(parentNote);
        }
        NoteEntity noteEntity = new NoteEntity();
        noteEntity.setParentNote(parentNote);
        model.addAttribute("noteEntity", noteEntity);
        return "notes/new";
    }

    @PostMapping("")
    public String createNote(@Valid NoteEntity noteEntity, BindingResult bindingResult, Model model) throws NoAccessRightException {
        if (bindingResult.hasErrors()) {
            return "notes/new";
        }
        if (noteEntity.getParentNote() != null) {
            currentUserFetcher.getUser().assertAuthorization(noteEntity.getParentNote());
        }
        UserModel userModel = currentUserFetcher.getUser();
        noteEntity.setUserEntity(userModel.getEntity());
        modelFactoryService.noteRepository.save(noteEntity);
        return "redirect:/notes/" + noteEntity.getId();
    }

    @GetMapping("/{noteEntity}")
    public String showNote(@PathVariable(name = "noteEntity") NoteEntity noteEntity, Model model) {
        model.addAttribute("treeNodeModel", modelFactoryService.toTreeNodeModel(noteEntity));
        return "notes/show";
    }

    @GetMapping("/{noteEntity}/edit")
    public String editNote(NoteEntity noteEntity) {
        return "notes/edit";
    }

    @PostMapping("/{noteEntity}")
    public String updateNote(@Valid NoteEntity noteEntity, BindingResult bindingResult) throws NoAccessRightException {
        currentUserFetcher.getUser().assertAuthorization(noteEntity);
        if (bindingResult.hasErrors()) {
            return "notes/edit";
        }
        modelFactoryService.noteRepository.save(noteEntity);
        return "redirect:/notes/" + noteEntity.getId();
    }

    @GetMapping("/{noteEntity}/link")
    public String link( @PathVariable("noteEntity") NoteEntity noteEntity, @RequestParam(required = false) String searchTerm, Model model) {
        List<NoteEntity> linkableNotes = currentUserFetcher.getUser().filterLinkableNotes(noteEntity, searchTerm);
        model.addAttribute("linkableNotes", linkableNotes);
        return "notes/link";
    }

    @PostMapping(value = "/{noteEntity}/link", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String linkNote(@PathVariable("noteEntity") NoteEntity noteEntity, Integer targetNoteId) throws NoAccessRightException {
        currentUserFetcher.getUser().assertAuthorization(noteEntity);
        NoteEntity targetNote = modelFactoryService.noteRepository.findById(targetNoteId).get();
        NoteContentModel noteContentModel = modelFactoryService.toNoteModel(noteEntity);
        noteContentModel.linkNote(targetNote);
        return "redirect:/notes/" + noteEntity.getId();
    }

    @GetMapping("/{noteEntity}/move")
    public String prepareToMove(NoteEntity noteEntity, Model model) {
        model.addAttribute("noteMotion", getLeftNoteMotion(noteEntity));
        model.addAttribute("noteMotionRight", getRightNoteMotion(noteEntity));
        model.addAttribute("noteMotionUnder", new NoteMotionEntity(null, true));
        return "notes/move";
    }

    private NoteMotionEntity getLeftNoteMotion(NoteEntity noteEntity) {
        TreeNodeModel treeNodeModel = this.modelFactoryService.toTreeNodeModel(noteEntity);
        NoteEntity previousSiblingNote = treeNodeModel.getPreviousSiblingNote();
        TreeNodeModel prev = this.modelFactoryService.toTreeNodeModel(previousSiblingNote);
        NoteEntity prevprev = prev.getPreviousSiblingNote();
        if (prevprev == null) {
            return new NoteMotionEntity(noteEntity.getParentNote(), true);
        }
        return new NoteMotionEntity(prevprev, false);
    }

    private NoteMotionEntity getRightNoteMotion(NoteEntity noteEntity) {
        TreeNodeModel treeNodeModel = this.modelFactoryService.toTreeNodeModel(noteEntity);
        return new NoteMotionEntity(treeNodeModel.getNextSiblingNote(), false);
    }

    @PostMapping("/{noteEntity}/move")
    public String moveNote(NoteEntity noteEntity, NoteMotionEntity noteMotionEntity) throws CyclicLinkDetectedException, NoAccessRightException {
        currentUserFetcher.getUser().assertAuthorization(noteEntity);
        currentUserFetcher.getUser().assertAuthorization(noteMotionEntity.getRelativeToNote());
        modelFactoryService.toNoteMotionModel(noteMotionEntity, noteEntity).execute();
        return "redirect:/notes/" + noteEntity.getId();
    }

    @PostMapping(value = "/{noteEntity}/delete")
    @Transactional
    public RedirectView deleteNote(@PathVariable("noteEntity") NoteEntity noteEntity) throws NoAccessRightException {
        currentUserFetcher.getUser().assertAuthorization(noteEntity);
        modelFactoryService.toNoteModel(noteEntity).destroy();
        return new RedirectView("/notes");
    }

}
