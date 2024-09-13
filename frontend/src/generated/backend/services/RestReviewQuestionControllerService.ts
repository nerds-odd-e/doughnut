/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AnswerDTO } from '../models/AnswerDTO';
import type { AnsweredQuestion } from '../models/AnsweredQuestion';
import type { ReviewQuestionContestResult } from '../models/ReviewQuestionContestResult';
import type { ReviewQuestionInstance } from '../models/ReviewQuestionInstance';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestReviewQuestionControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
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
            url: '/api/review-questions/{reviewQuestionInstance}/regenerate',
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
     * @returns ReviewQuestionContestResult OK
     * @throws ApiError
     */
    public contest(
        reviewQuestionInstance: number,
    ): CancelablePromise<ReviewQuestionContestResult> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/review-questions/{reviewQuestionInstance}/contest',
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
            url: '/api/review-questions/{reviewQuestionInstance}/answer',
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
     * @returns ReviewQuestionInstance OK
     * @throws ApiError
     */
    public generateQuestion(
        note: number,
    ): CancelablePromise<ReviewQuestionInstance> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/review-questions/generate-question',
            query: {
                'note': note,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param reviewPoint
     * @returns ReviewQuestionInstance OK
     * @throws ApiError
     */
    public generateRandomQuestion(
        reviewPoint: number,
    ): CancelablePromise<ReviewQuestionInstance> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/review-questions/{reviewPoint}/random-question',
            path: {
                'reviewPoint': reviewPoint,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
