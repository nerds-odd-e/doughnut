package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.models.*;
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
@RequestMapping("/notes")
public class NoteController extends ApplicationMvcController  {
    private final ModelFactoryService modelFactoryService;

    public NoteController(CurrentUserFetcher currentUserFetcher, ModelFactoryService modelFactoryService) {
        super(currentUserFetcher);
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("/{parentNote}/new")
    public String newNote(@PathVariable(name = "parentNote") Note parentNote, Model model) throws NoAccessRightException {
        UserModel userModel = getCurrentUser();
        userModel.assertAuthorization(parentNote);
        NoteContent noteContent = new NoteContent();
        model.addAttribute("ownershipEntity", userModel.getEntity().getOwnershipEntity());
        model.addAttribute("noteContent", noteContent);
        return "notes/new";
    }

    @PostMapping("/{parentNote}/create")
    public String createNote(@PathVariable(name = "parentNote") Note parentNote, @Valid NoteContent noteContent, BindingResult bindingResult, Model model) throws NoAccessRightException, IOException {
        if (bindingResult.hasErrors()) {
            return "notes/new";
        }
        UserModel userModel = getCurrentUser();
        userModel.assertAuthorization(parentNote);
        Note note = new Note();
        UserEntity userEntity = userModel.getEntity();
        note.updateNoteContent(noteContent, userEntity);
        note.setParentNote(parentNote);
        note.setUserEntity(userEntity);
        modelFactoryService.noteRepository.save(note);
        return "redirect:/notes/" + note.getId();
    }

    @GetMapping("/{note}")
    public String showNote(@PathVariable(name = "note") Note note) throws NoAccessRightException {
        getCurrentUser().assertReadAuthorization(note);
        if (!getCurrentUser().hasFullAuthority(note)) {
            return "redirect:/bazaar/notes/" + note.getId();
        }
        return "notes/show";
    }

    @GetMapping("/{note}/edit")
    public String editNote(Note note, Model model) {
        model.addAttribute("noteContent", note.getNoteContent());
        return "notes/edit";
    }

    @PostMapping("/{note}")
    public String updateNote(@PathVariable(name = "note") Note note, @Valid NoteContent noteContent, BindingResult bindingResult) throws NoAccessRightException, IOException {
        getCurrentUser().assertAuthorization(note);
        if (bindingResult.hasErrors()) {
            return "notes/edit";
        }
        note.updateNoteContent(noteContent, getCurrentUser().getEntity());
        modelFactoryService.noteRepository.save(note);
        return "redirect:/notes/" + note.getId();
    }

    @GetMapping("/{note}/move")
    public String prepareToMove(Note note, Model model) {
        model.addAttribute("noteMotion", getLeftNoteMotion(note));
        model.addAttribute("noteMotionRight", getRightNoteMotion(note));
        model.addAttribute("noteMotionUnder", new NoteMotionEntity(null, true));
        return "notes/move";
    }

    private NoteMotionEntity getLeftNoteMotion(Note note) {
        Note previousSiblingNote = note.getPreviousSibling();
        if(previousSiblingNote != null) {
            Note prevprev = previousSiblingNote.getPreviousSibling();
            if (prevprev == null) {
                return new NoteMotionEntity(note.getParentNote(), true);
            }
            return new NoteMotionEntity(prevprev, false);
        }
        return new NoteMotionEntity(null, false);
    }

    private NoteMotionEntity getRightNoteMotion(Note note) {
        return new NoteMotionEntity(note.getNextSibling(), false);
    }

    @PostMapping("/{note}/move")
    @Transactional
    public String moveNote(Note note, NoteMotionEntity noteMotionEntity) throws CyclicLinkDetectedException, NoAccessRightException {
        getCurrentUser().assertAuthorization(note);
        getCurrentUser().assertAuthorization(noteMotionEntity.getRelativeToNote());
        modelFactoryService.toNoteMotionModel(noteMotionEntity, note).execute();
        return "redirect:/notes/" + note.getId();
    }

    @PostMapping(value = "/{note}/delete")
    @Transactional
    public RedirectView deleteNote(@PathVariable("note") Note note) throws NoAccessRightException {
        getCurrentUser().assertAuthorization(note);
        modelFactoryService.toTreeNodeModel(note).destroy();
        return new RedirectView("/notebooks");
    }

    @GetMapping("/{note}/review_setting")
    public String editReviewSetting(Note note, Model model) {
        ReviewSettingEntity reviewSettingEntity = note.getMasterReviewSettingEntity();
        if(reviewSettingEntity == null) {
            reviewSettingEntity = new ReviewSettingEntity();
        }
        model.addAttribute("reviewSettingEntity", reviewSettingEntity);
        return "notes/edit_review_setting";
    }

    @PostMapping(value = "/{note}/review_setting")
    @Transactional
    public String updateReviewSetting(@PathVariable("note") Note note, @Valid ReviewSettingEntity reviewSettingEntity, BindingResult bindingResult) throws NoAccessRightException {
        if (bindingResult.hasErrors()) {
            return "notes/edit_review_setting";
        }
        getCurrentUser().assertAuthorization(note);
        note.mergeMasterReviewSetting(reviewSettingEntity);
        modelFactoryService.noteRepository.save(note);

        return "redirect:/notes/" + note.getId();
    }

    private UserModel getCurrentUser() {
        return currentUserFetcher.getUser();
    }
}
