package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewSetting;
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
        ReviewPoint reviewPoint = reviewing.getOneInitialReviewPoint();
        if (reviewPoint == null) {
            return "redirect:/reviews";
        }
        model.addAttribute("reviewPoint", reviewPoint);
        model.addAttribute("reviewSetting", reviewing.getReviewSetting(reviewPoint.getNote()));
        return "reviews/initial";
    }

    @PostMapping(path="", params="submit")
    @Transactional
    public String create(@Valid ReviewPoint reviewPoint, @Valid ReviewSetting reviewSetting) {
        UserModel userModel = currentUserFetcher.getUser();
        ReviewPointModel reviewPointModel = modelFactoryService.toReviewPointModel(reviewPoint);
        reviewPointModel.initialReview(userModel, reviewSetting, timeTraveler.getCurrentUTCTimestamp());
        return "redirect:/reviews/initial";
    }

    @PostMapping(path="", params="skip")
    @Transactional
    public String skip(@Valid ReviewPoint reviewPoint, @Valid ReviewSetting reviewSetting) {
        UserModel userModel = currentUserFetcher.getUser();
        ReviewPointModel reviewPointModel = modelFactoryService.toReviewPointModel(reviewPoint);
        reviewPoint.setRemovedFromReview(true);
        reviewPointModel.initialReview(userModel, reviewSetting, timeTraveler.getCurrentUTCTimestamp());
        return "redirect:/reviews/initial";
    }

    @GetMapping("/repeat")
    public String repeatReview(Model model) {
        UserModel user = currentUserFetcher.getUser();
        Reviewing reviewing = user.createReviewing(timeTraveler.getCurrentUTCTimestamp());
        ReviewPointModel reviewPointModel = reviewing.getOneReviewPointNeedToRepeat(timeTraveler.getRandomizer());
        if(reviewPointModel != null) {
            model.addAttribute("reviewPoint", reviewPointModel.getEntity());
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

    @PostMapping("/{reviewPoint}/answer")
    public String answerQuiz(ReviewPoint reviewPoint, @Valid Answer answer, Model model) {
        AnswerModel answerModel = modelFactoryService.toAnswerModel(answer);
        model.addAttribute("answer", answerModel);
        return "reviews/repeat";
    }

    @PostMapping(path="/{reviewPoint}", params="remove")
    public String removeFromRepeating(@Valid ReviewPoint reviewPoint) {
        reviewPoint.setRemovedFromReview(true);
        modelFactoryService.reviewPointRepository.save(reviewPoint);
        return "redirect:/reviews/repeat";
    }

    @PostMapping(path="/{reviewPoint}", params="again")
    public String doRepeatAgain(@Valid ReviewPoint reviewPoint) {
        return "redirect:/reviews/repeat";
    }

    @PostMapping(path="/{reviewPoint}", params="satisfying")
    public String doRepeat(@Valid ReviewPoint reviewPoint) {
        modelFactoryService.toReviewPointModel(reviewPoint).repeat(timeTraveler.getCurrentUTCTimestamp());
        return "redirect:/reviews/repeat";
    }

    @PostMapping(path="/{reviewPoint}", params="sad")
    public String doRepeatSad(@Valid ReviewPoint reviewPoint) {
        modelFactoryService.toReviewPointModel(reviewPoint).repeatSad(timeTraveler.getCurrentUTCTimestamp());
        return "redirect:/reviews/repeat";
    }

    @PostMapping(path="/{reviewPoint}", params="happy")
    public String doRepeatHappy(@Valid ReviewPoint reviewPoint) {
        modelFactoryService.toReviewPointModel(reviewPoint).repeatHappy(timeTraveler.getCurrentUTCTimestamp());
        return "redirect:/reviews/repeat";
    }

}
