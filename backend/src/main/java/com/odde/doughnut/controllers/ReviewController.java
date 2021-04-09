package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.AnswerEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.entities.ReviewSettingEntity;
import com.odde.doughnut.models.*;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.TimeTraveler;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.validation.Valid;

@Controller
@RequestMapping("/reviews")
public class ReviewController extends ApplicationMvcController  {
    private final ModelFactoryService modelFactoryService;


    @Resource(name = "timeTraveler")
    private final TimeTraveler timeTraveler;

    public ReviewController(CurrentUserFetcher currentUserFetcher, ModelFactoryService modelFactoryService, TimeTraveler timeTraveler) {
        super(currentUserFetcher);
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
    public String initialReview(Model model) {
        UserModel user = currentUserFetcher.getUser();
        Reviewing reviewing = user.createReviewing(timeTraveler.getCurrentUTCTimestamp());
        ReviewPointEntity reviewPointEntity = reviewing.getOneInitialReviewPointEntity();
        if (reviewPointEntity == null) {
            return "redirect:/reviews";
        }
        model.addAttribute("reviewPointEntity", reviewPointEntity);
        model.addAttribute("reviewSettingEntity", reviewing.getReviewSettingEntity(reviewPointEntity.getNote()));
        return "reviews/initial";
    }

    @PostMapping(path="", params="submit")
    @Transactional
    public String create(@Valid ReviewPointEntity reviewPointEntity, @Valid ReviewSettingEntity reviewSettingEntity) {
        UserModel userModel = currentUserFetcher.getUser();
        ReviewPointModel reviewPointModel = modelFactoryService.toReviewPointModel(reviewPointEntity);
        reviewPointModel.initialReview(userModel, reviewSettingEntity, timeTraveler.getCurrentUTCTimestamp());
        return "redirect:/reviews/initial";
    }

    @PostMapping(path="", params="skip")
    @Transactional
    public String skip(@Valid ReviewPointEntity reviewPointEntity, @Valid ReviewSettingEntity reviewSettingEntity) {
        UserModel userModel = currentUserFetcher.getUser();
        ReviewPointModel reviewPointModel = modelFactoryService.toReviewPointModel(reviewPointEntity);
        reviewPointEntity.setRemovedFromReview(true);
        reviewPointModel.initialReview(userModel, reviewSettingEntity, timeTraveler.getCurrentUTCTimestamp());
        return "redirect:/reviews/initial";
    }

    @GetMapping("/repeat")
    public String repeatReview(Model model) {
        UserModel user = currentUserFetcher.getUser();
        Reviewing reviewing = user.createReviewing(timeTraveler.getCurrentUTCTimestamp());
        ReviewPointModel reviewPointModel = reviewing.getOneReviewPointNeedToRepeat(timeTraveler.getRandomizer());
        if(reviewPointModel != null) {
            model.addAttribute("reviewPointEntity", reviewPointModel.getEntity());
            QuizQuestion quizQuestion = reviewPointModel.generateAQuizQuestion(timeTraveler.getRandomizer());
            if (quizQuestion == null) {
                return "reviews/repeat";
            }
            model.addAttribute("quizQuestion", quizQuestion);
            model.addAttribute("emptyAnswer", quizQuestion.buildAnswer());
            return "reviews/quiz";
        }
        return "redirect:/reviews";
    }

    @PostMapping("/{reviewPointEntity}/answer")
    public String answerQuiz(ReviewPointEntity reviewPointEntity, @Valid AnswerEntity answerEntity, Model model) {
        AnswerModel answerModel = modelFactoryService.toAnswerModel(answerEntity);
        model.addAttribute("answer", answerModel);
        return "reviews/repeat";
    }

    @PostMapping(path="/{reviewPointEntity}", params="remove")
    public String removeFromRepeating(@Valid ReviewPointEntity reviewPointEntity) {
        reviewPointEntity.setRemovedFromReview(true);
        modelFactoryService.reviewPointRepository.save(reviewPointEntity);
        return "redirect:/reviews/repeat";
    }

    @PostMapping(path="/{reviewPointEntity}", params="again")
    public String doRepeatAgain(@Valid ReviewPointEntity reviewPointEntity) {
        return "redirect:/reviews/repeat";
    }

    @PostMapping(path="/{reviewPointEntity}", params="satisfying")
    public String doRepeat(@Valid ReviewPointEntity reviewPointEntity) {
        modelFactoryService.toReviewPointModel(reviewPointEntity).repeat(timeTraveler.getCurrentUTCTimestamp());
        return "redirect:/reviews/repeat";
    }

    @PostMapping(path="/{reviewPointEntity}", params="sad")
    public String doRepeatSad(@Valid ReviewPointEntity reviewPointEntity) {
        modelFactoryService.toReviewPointModel(reviewPointEntity).repeatSad(timeTraveler.getCurrentUTCTimestamp());
        return "redirect:/reviews/repeat";
    }

    @PostMapping(path="/{reviewPointEntity}", params="happy")
    public String doRepeatHappy(@Valid ReviewPointEntity reviewPointEntity) {
        modelFactoryService.toReviewPointModel(reviewPointEntity).repeatHappy(timeTraveler.getCurrentUTCTimestamp());
        return "redirect:/reviews/repeat";
    }

}
