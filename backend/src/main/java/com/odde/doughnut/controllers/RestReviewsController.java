
package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.RepetitionForUser;
import com.odde.doughnut.entities.json.ReviewPointViewedByUser;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.ReviewPointModel;
import com.odde.doughnut.models.Reviewing;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/reviews")
class RestReviewsController {
    private final ModelFactoryService modelFactoryService;
    private final CurrentUserFetcher currentUserFetcher;
    @Resource(name = "testabilitySettings")
    private final TestabilitySettings testabilitySettings;


    public RestReviewsController(ModelFactoryService modelFactoryService, CurrentUserFetcher currentUserFetcher, TestabilitySettings testabilitySettings) {
        this.modelFactoryService = modelFactoryService;
        this.currentUserFetcher = currentUserFetcher;
        this.testabilitySettings = testabilitySettings;
    }

    @GetMapping("/overview")
    public Reviewing overview() {
        UserModel user = currentUserFetcher.getUser();
        user.getAuthorization().assertLoggedIn();
        return user.createReviewing(testabilitySettings.getCurrentUTCTimestamp());
    }

    @GetMapping("/initial")
    public ReviewPointViewedByUser initialReview() {
        UserModel user = currentUserFetcher.getUser();
        user.getAuthorization().assertLoggedIn();
        Reviewing reviewing = user.createReviewing(testabilitySettings.getCurrentUTCTimestamp());
        ReviewPoint reviewPoint = reviewing.getOneInitialReviewPoint();
        ReviewPointViewedByUser from = ReviewPointViewedByUser.from(reviewPoint, user);
        from.setRemainingInitialReviewCountForToday(reviewing.toInitialReviewCount());
        return from;
    }

    static class InitialInfo {
        @Valid
        public ReviewPoint reviewPoint;
        @Valid
        public ReviewSetting reviewSetting;
    }

    @PostMapping(path = "")
    @Transactional
    public ReviewPointViewedByUser create(@RequestBody InitialInfo initialInfo) {
        UserModel userModel = currentUserFetcher.getUser();
        userModel.getAuthorization().assertLoggedIn();
        if (initialInfo.reviewPoint.getNoteId() != null) {
            initialInfo.reviewPoint.setNote(modelFactoryService.noteRepository.findById(initialInfo.reviewPoint.getNoteId()).orElse(null));
        }
        if (initialInfo.reviewPoint.getLinkId() != null) {
            initialInfo.reviewPoint.setLink(modelFactoryService.linkRepository.findById(initialInfo.reviewPoint.getLinkId()).orElse(null));
        }
        ReviewPointModel reviewPointModel = modelFactoryService.toReviewPointModel(initialInfo.reviewPoint);
        reviewPointModel.initialReview(userModel, initialInfo.reviewSetting, testabilitySettings.getCurrentUTCTimestamp());
        return initialReview();
    }

    @GetMapping("/repeat")
    public RepetitionForUser repeatReview() {
        UserModel user = currentUserFetcher.getUser();
        user.getAuthorization().assertLoggedIn();
        Reviewing reviewing = user.createReviewing(testabilitySettings.getCurrentUTCTimestamp());
        ReviewPointModel reviewPointModel = reviewing.getOneReviewPointNeedToRepeat(testabilitySettings.getRandomizer());

        RepetitionForUser repetitionForUser = new RepetitionForUser();

        if (reviewPointModel != null) {
            repetitionForUser.setReviewPointViewedByUser(ReviewPointViewedByUser.from(reviewPointModel.getEntity(), user));
            QuizQuestion quizQuestion = reviewPointModel.generateAQuizQuestion(testabilitySettings.getRandomizer());
            if (quizQuestion != null) {
                repetitionForUser.setQuizQuestion(quizQuestion);
                repetitionForUser.setEmptyAnswer(quizQuestion.buildAnswer());
            }
        }
        repetitionForUser.setToRepeatCount(reviewing.toRepeatCount());
        return repetitionForUser;
    }

    @PostMapping("/{reviewPoint}/answer")
    @Transactional
    public AnswerResult answerQuiz(ReviewPoint reviewPoint, @Valid @RequestBody Answer answer) {
        UserModel user = currentUserFetcher.getUser();
        user.getAuthorization().assertLoggedIn();
        AnswerResult answerResult = new AnswerResult();
        answerResult.setReviewPoint(reviewPoint);
        answerResult.setQuestionType(answer.getQuestionType());
        answerResult.setAnswer(answer.getAnswer());
        if (answer.getAnswerNoteId() != null) {
            answerResult.setAnswerNote(modelFactoryService.noteRepository.findById(answer.getAnswerNoteId()).orElse(null));
        }
        if (answer.getViceReviewPointIds() != null) {
            answer.getViceReviewPointIds().forEach(rPid ->
                    modelFactoryService.reviewPointRepository
                            .findById(rPid).ifPresent(vice -> updateReviewPoint(vice, answerResult))
            );
        }
        updateReviewPoint(reviewPoint, answerResult);
        return answerResult;
    }

    private void updateReviewPoint(ReviewPoint reviewPoint, final AnswerResult answerResult) {
        modelFactoryService.toReviewPointModel(reviewPoint).increaseRepetitionCountAndSave();
        if (answerResult.isCorrect()) {
            modelFactoryService.toReviewPointModel(reviewPoint).repeated(testabilitySettings.getCurrentUTCTimestamp());
        }
    }

    static class SelfEvaluation {
        public String selfEvaluation;
        public Boolean increaseRepeatCount;
    }

    @PostMapping(path = "/{reviewPoint}/self-evaluate")
    public RepetitionForUser selfEvaluate(ReviewPoint reviewPoint, @RequestBody SelfEvaluation selfEvaluation) {
        if (reviewPoint == null || reviewPoint.getId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The review point does not exist.");
        }
        UserModel user = currentUserFetcher.getUser();
        user.getAuthorization().assertLoggedIn();
        evaluate(reviewPoint, selfEvaluation);
        return repeatReview();
    }

    private void evaluate(ReviewPoint reviewPoint, SelfEvaluation selfEvaluation) {
        ReviewPointModel reviewPointModel = modelFactoryService.toReviewPointModel(reviewPoint);
        if (selfEvaluation.increaseRepeatCount != null && selfEvaluation.increaseRepeatCount) {
            reviewPointModel.increaseRepetitionCountAndSave();
        }
        if ("again".equals(selfEvaluation.selfEvaluation)) {
            return;
        }
        if ("satisfying".equals(selfEvaluation.selfEvaluation)) {
            reviewPointModel.repeated(testabilitySettings.getCurrentUTCTimestamp());
            return;
        }
        if ("sad".equals(selfEvaluation.selfEvaluation)) {
            reviewPointModel.repeatedSad(testabilitySettings.getCurrentUTCTimestamp());
            return;
        }
        if ("happy".equals(selfEvaluation.selfEvaluation)) {
            reviewPointModel.repeatedHappy(testabilitySettings.getCurrentUTCTimestamp());
            return;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

}
