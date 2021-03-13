package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.models.ReviewPointModel;
import com.odde.doughnut.models.Reviewing;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.TimeTraveler;
import org.apache.logging.log4j.util.Strings;
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
        Reviewing reviewing = user.createReviewing(timeTraveler.getCurrentUTCTimestamp());
        model.addAttribute("reviewing", reviewing);
        return "reviews/index";
    }

    @GetMapping("/initial")
    public String review(Model model) {
        UserModel user = currentUserFetcher.getUser();
        Reviewing reviewing = user.createReviewing(timeTraveler.getCurrentUTCTimestamp());
        ReviewPointEntity reviewPointEntity = reviewing.getOneInitialReviewPointEntity();
        if (reviewPointEntity == null) {
            return "redirect:/reviews";
        }
        model.addAttribute("reviewPointEntity", reviewPointEntity);
        return "reviews/initial";
    }

    @PostMapping("")
    public String create(@Valid ReviewPointEntity reviewPointEntity) {
        UserModel userModel = currentUserFetcher.getUser();
        ReviewPointModel reviewPointModel = modelFactoryService.toReviewPointModel(reviewPointEntity);
        reviewPointModel.initialReview(userModel, timeTraveler.getCurrentUTCTimestamp());
        return "redirect:/reviews/initial";
    }

    @GetMapping("/repeat")
    public String repeatReview(Model model) {
        UserModel user = currentUserFetcher.getUser();
        Reviewing reviewing = user.createReviewing(timeTraveler.getCurrentUTCTimestamp());
        ReviewPointModel reviewPointModel = reviewing.getOneReviewPointNeedToRepeat();
        if(reviewPointModel != null) {
            model.addAttribute("reviewPointEntity", reviewPointModel.getEntity());
            if (Strings.isEmpty(reviewPointModel.getEntity().getNoteEntity().getDescription())) {
                return "reviews/repeat";
            }
            return "reviews/quiz";
        }
        return "redirect:/reviews";
    }

    @PostMapping("/{reviewPointEntity}/answer")
    public String answerQuiz(ReviewPointEntity reviewPointEntity, Model model) {
        model.addAttribute("answer", true);
        return "reviews/repeat";
    }

    @PostMapping("/{reviewPointEntity}")
    public String update(@Valid ReviewPointEntity reviewPointEntity) {
        ReviewPointModel reviewPointModel = modelFactoryService.toReviewPointModel(reviewPointEntity);
        reviewPointModel.repeat(timeTraveler.getCurrentUTCTimestamp());
        return "redirect:/reviews/repeat";
    }

}
