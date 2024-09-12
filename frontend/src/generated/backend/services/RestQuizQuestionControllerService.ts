/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AnswerDTO } from '../models/AnswerDTO';
import type { AnsweredQuestion } from '../models/AnsweredQuestion';
import type { PredefinedQuestion } from '../models/PredefinedQuestion';
import type { QuestionSuggestionCreationParams } from '../models/QuestionSuggestionCreationParams';
import type { QuizQuestionContestResult } from '../models/QuizQuestionContestResult';
import type { ReviewQuestionInstance } from '../models/ReviewQuestionInstance';
import type { SuggestedQuestionForFineTuning } from '../models/SuggestedQuestionForFineTuning';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestQuizQuestionControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param reviewQuestionInstance
     * @returns PredefinedQuestion OK
     * @throws ApiError
     */
    public toggleApproval(
        reviewQuestionInstance: number,
    ): CancelablePromise<PredefinedQuestion> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/quiz-questions/{reviewQuestionInstance}/toggle-approval',
            path: {
                'reviewQuestionInstance': reviewQuestionInstance,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param reviewQuestionInstance
     * @param requestBody
     * @returns SuggestedQuestionForFineTuning OK
     * @throws ApiError
     */
    public suggestQuestionForFineTuning(
        reviewQuestionInstance: number,
        requestBody: QuestionSuggestionCreationParams,
    ): CancelablePromise<SuggestedQuestionForFineTuning> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/quiz-questions/{reviewQuestionInstance}/suggest-fine-tuning',
            path: {
                'reviewQuestionInstance': reviewQuestionInstance,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param reviewQuestionInstance
     * @returns ReviewQuestionInstance OK
     * @throws ApiError
     */
    public regenerate(
        reviewQuestionInstance: number,
    ): CancelablePromise<ReviewQuestionInstance> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/quiz-questions/{reviewQuestionInstance}/regenerate',
            path: {
                'reviewQuestionInstance': reviewQuestionInstance,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param reviewQuestionInstance
     * @returns QuizQuestionContestResult OK
     * @throws ApiError
     */
    public contest(
        reviewQuestionInstance: number,
    ): CancelablePromise<QuizQuestionContestResult> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/quiz-questions/{reviewQuestionInstance}/contest',
            path: {
                'reviewQuestionInstance': reviewQuestionInstance,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param reviewQuestionInstance
     * @param requestBody
     * @returns AnsweredQuestion OK
     * @throws ApiError
     */
    public answerQuiz(
        reviewQuestionInstance: number,
        requestBody: AnswerDTO,
    ): CancelablePromise<AnsweredQuestion> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/quiz-questions/{reviewQuestionInstance}/answer',
            path: {
                'reviewQuestionInstance': reviewQuestionInstance,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param note
     * @param requestBody
     * @returns PredefinedQuestion OK
     * @throws ApiError
     */
    public refineQuestion(
        note: number,
        requestBody: PredefinedQuestion,
    ): CancelablePromise<PredefinedQuestion> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/quiz-questions/{note}/refine-question',
            path: {
                'note': note,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param note
     * @returns PredefinedQuestion OK
     * @throws ApiError
     */
    public getAllQuestionByNote(
        note: number,
    ): CancelablePromise<Array<PredefinedQuestion>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/quiz-questions/{note}/note-questions',
            path: {
                'note': note,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param note
     * @param requestBody
     * @returns PredefinedQuestion OK
     * @throws ApiError
     */
    public addQuestionManually(
        note: number,
        requestBody: PredefinedQuestion,
    ): CancelablePromise<PredefinedQuestion> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/quiz-questions/{note}/note-questions',
            path: {
                'note': note,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param note
     * @returns ReviewQuestionInstance OK
     * @throws ApiError
     */
    public generateQuestion(
        note: number,
    ): CancelablePromise<ReviewQuestionInstance> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/quiz-questions/generate-question',
            query: {
                'note': note,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param note
     * @returns PredefinedQuestion OK
     * @throws ApiError
     */
    public generateAiQuestionWithoutSave(
        note: number,
    ): CancelablePromise<PredefinedQuestion> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/quiz-questions/generate-question-without-save',
            query: {
                'note': note,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
