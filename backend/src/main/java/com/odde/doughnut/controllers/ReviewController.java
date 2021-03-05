package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.models.ReviewPointModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.TimeTraveler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.validation.Valid;

@Controller
@RequestMapping("/reviews")
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

    @GetMapping("")
    public String index(Model model) {
        UserModel user = currentUserFetcher.getUser();
        model.addAttribute("oldEntity", user.getReviewPointNeedToRepeat(timeTraveler.getCurrentUTCTimestamp()));
        model.addAttribute("newEntity", user.getOneInitialReviewPointEntity(timeTraveler.getCurrentUTCTimestamp()));
        return "reviews/index";
    }

    @GetMapping("/initial")
    public String review(Model model) {
        UserModel user = currentUserFetcher.getUser();
        ReviewPointEntity reviewPointEntity = user.getOneInitialReviewPointEntity(timeTraveler.getCurrentUTCTimestamp());
        if (reviewPointEntity == null) {
            return "redirect:/reviews";
        }
        model.addAttribute("reviewPointEntity", reviewPointEntity);
        return "reviews/initial";
    }

    @GetMapping("/repeat")
    public String repeatReview(Model model) {
        UserModel user = currentUserFetcher.getUser();
        ReviewPointEntity reviewPointEntity = user.getReviewPointNeedToRepeat(timeTraveler.getCurrentUTCTimestamp());
        if(reviewPointEntity != null) {
            model.addAttribute("reviewPointEntity", reviewPointEntity);
            return "reviews/repeat";
        }
        return "redirect:/reviews";
    }

    @PostMapping("")
    public String create(@Valid ReviewPointEntity reviewPointEntity) {
        UserModel userModel = currentUserFetcher.getUser();
        ReviewPointModel reviewPointModel = modelFactoryService.toReviewPointModel(reviewPointEntity);
        reviewPointModel.initalReview(userModel, timeTraveler.getCurrentUTCTimestamp());
        return "redirect:/reviews/initial";
    }

    @PostMapping("/{reviewPointEntity}")
    public String update(@Valid ReviewPointEntity reviewPointEntity) {
        reviewPointEntity.setLastReviewedAt(timeTraveler.getCurrentUTCTimestamp());
        reviewPointEntity.repeatedOnTime();
        modelFactoryService.reviewPointRepository.save(reviewPointEntity);
        return "redirect:/reviews/repeat";
    }

}
