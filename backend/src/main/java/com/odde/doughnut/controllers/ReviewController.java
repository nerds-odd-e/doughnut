package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ReviewController {
    private final CurrentUser currentUser;
    private final ModelFactoryService modelFactoryService;

    public ReviewController(CurrentUser currentUser, ModelFactoryService modelFactoryService) {
        this.currentUser = currentUser;
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("/review")
    public String review(Model model) {
        List<NoteEntity> notes = currentUser.getUser().getNotesInDescendingOrder();
        if (notes.size() > 0) {
            NoteEntity noteEntity = notes.get(0);
            if (modelFactoryService.reviewPointRepository.findByNoteEntity(noteEntity) != null) {
                return "review_done";
            }
            ReviewPointEntity reviewPointEntity = new ReviewPointEntity();
            reviewPointEntity.setNoteEntity(noteEntity);
            reviewPointEntity.setUserEntity(currentUser.getUser());
            modelFactoryService.reviewPointRepository.save(reviewPointEntity);
        }
        model.addAttribute("notes", notes);
        return "review";
    }
}
