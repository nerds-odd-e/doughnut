package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.TimeTraveler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@Controller
public class ReviewController {
    private final CurrentUserFetcher currentUserFetcher;
    private final ModelFactoryService modelFactoryService;


    @Resource(name = "timeTraveler")
    private final TimeTraveler timeTraveler;

    public ReviewController(CurrentUserFetcher currentUserFetcher, ModelFactoryService modelFactoryService, TimeTraveler timeTraveler) {
        this.currentUserFetcher = currentUserFetcher;
        this.modelFactoryService = modelFactoryService;
        this.timeTraveler = timeTraveler;
    }

    @GetMapping("/reviews/initial")
    public String review(Model model) {
        List<NoteEntity> notes = currentUserFetcher.getUser().getNewNotesToReview(timeTraveler.getCurrentUTCTimestamp());
        if (notes.size() == 0) {
            return "reviews/initial_done";
        }
        ReviewPointEntity reviewPointEntity = new ReviewPointEntity();
        reviewPointEntity.setNoteEntity(notes.get(0));
        model.addAttribute("reviewPointEntity", reviewPointEntity);
        return "reviews/initial";
    }

    @GetMapping("/reviews/repeat")
    public String repeatReview(Model model) {
        List<NoteEntity> notes = currentUserFetcher.getUser().getNewNotesToReview(timeTraveler.getCurrentUTCTimestamp());
        if (notes.size() == 0) {
            return "reviews/initial_done";
        }
        ReviewPointEntity reviewPointEntity = new ReviewPointEntity();
        reviewPointEntity.setNoteEntity(notes.get(0));
        model.addAttribute("reviewPointEntity", reviewPointEntity);
        return "reviews/initial";
    }

    @PostMapping("/reviews")
    public String create(@Valid ReviewPointEntity reviewPointEntity) {
        UserModel userModel = currentUserFetcher.getUser();
        reviewPointEntity.setLastReviewedAt(timeTraveler.getCurrentUTCTimestamp());
        reviewPointEntity.setUserEntity(userModel.getEntity());
        modelFactoryService.reviewPointRepository.save(reviewPointEntity);
        return "redirect:/reviews/initial";
    }

}
