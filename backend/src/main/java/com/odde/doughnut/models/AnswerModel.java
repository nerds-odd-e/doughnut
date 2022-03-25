package com.odde.doughnut.models;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.SelfEvaluation;
import com.odde.doughnut.factoryServices.ModelFactoryService;

import java.sql.Timestamp;
import java.util.List;

public class AnswerModel {
    private final Answer answer;
    private final ModelFactoryService modelFactoryService;
    private final QuizQuestion.QuestionType questionType;
    private final ReviewPoint reviewPoint;
    private Boolean cachedResult;

    public AnswerModel(Answer answer, ModelFactoryService modelFactoryService) {
        this.answer = answer;
        this.modelFactoryService = modelFactoryService;
        this.questionType = answer.getQuestion().getQuestionType();
        this.reviewPoint = answer.getQuestion().getReviewPoint();
    }

    public void updateReviewPoints(Timestamp currentUTCTimestamp) {
        boolean correct = isCorrect();
        answer.getQuestion().getViceReviewPointIdList().forEach(rPid ->
                this.modelFactoryService.reviewPointRepository
                        .findById(rPid).ifPresent(vice -> this.modelFactoryService.toReviewPointModel(vice).updateReviewPoint(correct, currentUTCTimestamp))
        );
        ReviewPointModel reviewPointModel = this.modelFactoryService.toReviewPointModel(answer.getQuestion().getReviewPoint());
        if(answer.getQuestion().getQuestionType() == QuizQuestion.QuestionType.JUST_REVIEW) {
            reviewPointModel.evaluate(currentUTCTimestamp, answer.getSpellingAnswer(), true);
            return;
        }
        reviewPointModel.updateReviewPoint(correct, currentUTCTimestamp);
    }

    public AnswerViewedByUser getAnswerViewedByUser() {
        AnswerViewedByUser answerResult = new AnswerViewedByUser();
        answerResult.answerId = answer.getId();
        answerResult.correct = isCorrect();
        answerResult.answerDisplay = getAnswerDisplay();
        return answerResult;
    }

    public AnswerResult getAnswerResult() {
        AnswerResult answerResult = new AnswerResult();
        answerResult.answerId = answer.getId();
        answerResult.correct = isCorrect();
        return answerResult;
    }

    public void save() {
        modelFactoryService.answerRepository.save(answer);
    }

    private String getAnswerDisplay() {
        if (getAnswerNote() != null) {
            return getAnswerNote().getTitle();
        }
        return answer.getSpellingAnswer();
    }

    private boolean isCorrect() {
        if (cachedResult != null) return cachedResult;
        List<Note> wrongAnswers = questionType.factory.apply(reviewPoint).allWrongAnswers();
        if (wrongAnswers != null) {
            return wrongAnswers.stream().noneMatch(this::matchAnswer);
        }
        List<Note> rightAnswers = questionType.factory.apply(reviewPoint).knownRightAnswers();
        cachedResult = rightAnswers.stream().anyMatch(this::matchAnswer);
        return cachedResult;
    }

    private Note getAnswerNote() {
        if (answer.getAnswerNoteId() == null) return null;
        return this.modelFactoryService.noteRepository.findById(answer.getAnswerNoteId()).orElse(null);
    }

    private boolean matchAnswer(Note correctAnswerNote) {
        if (getAnswerNote() != null) {
            return correctAnswerNote.equals(getAnswerNote());
        }

        return correctAnswerNote.getNoteTitle().matches(answer.getSpellingAnswer());
    }
}

