package com.odde.doughnut.models;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.factoryServices.ModelFactoryService;

import java.sql.Timestamp;

public class AnswerModel {
    private final Answer answer;
    private final ModelFactoryService modelFactoryService;

    public AnswerModel(Answer answer, ModelFactoryService modelFactoryService) {
        this.answer = answer;
        this.modelFactoryService = modelFactoryService;
    }

    public void updateReviewPoints(Timestamp currentUTCTimestamp, boolean correct) {
        answer.getQuestion().getViceReviewPointIdList().forEach(rPid ->
                this.modelFactoryService.reviewPointRepository
                        .findById(rPid).ifPresent(vice -> this.modelFactoryService.toReviewPointModel(vice).updateReviewPoint(correct, currentUTCTimestamp))
        );
        this.modelFactoryService.toReviewPointModel(answer.getQuestion().getReviewPoint()).updateReviewPoint(correct, currentUTCTimestamp);
    }
}
