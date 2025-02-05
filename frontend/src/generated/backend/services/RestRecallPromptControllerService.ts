/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AnswerDTO } from '../models/AnswerDTO';
import type { AnsweredQuestion } from '../models/AnsweredQuestion';
import type { QuestionContestResult } from '../models/QuestionContestResult';
import type { RecallPrompt } from '../models/RecallPrompt';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestRecallPromptControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param recallPrompt
     * @param requestBody
     * @returns RecallPrompt OK
     * @throws ApiError
     */
    public regenerate(
        recallPrompt: number,
        requestBody: QuestionContestResult,
    ): CancelablePromise<RecallPrompt> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/recall-prompts/{recallPrompt}/regenerate',
            path: {
                'recallPrompt': recallPrompt,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param recallPrompt
     * @returns QuestionContestResult OK
     * @throws ApiError
     */
    public contest(
        recallPrompt: number,
    ): CancelablePromise<QuestionContestResult> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/recall-prompts/{recallPrompt}/contest',
            path: {
                'recallPrompt': recallPrompt,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param recallPrompt
     * @param requestBody
     * @returns AnsweredQuestion OK
     * @throws ApiError
     */
    public answerQuiz(
        recallPrompt: number,
        requestBody: AnswerDTO,
    ): CancelablePromise<AnsweredQuestion> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/recall-prompts/{recallPrompt}/answer',
            path: {
                'recallPrompt': recallPrompt,
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
     * @returns RecallPrompt OK
     * @throws ApiError
     */
    public generateQuestion(
        note: number,
    ): CancelablePromise<RecallPrompt> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/recall-prompts/generate-question',
            query: {
                'note': note,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param recallPrompt
     * @returns AnsweredQuestion OK
     * @throws ApiError
     */
    public showQuestion(
        recallPrompt: number,
    ): CancelablePromise<AnsweredQuestion> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/recall-prompts/{recallPrompt}',
            path: {
                'recallPrompt': recallPrompt,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param memoryTracker
     * @returns RecallPrompt OK
     * @throws ApiError
     */
    public generateRandomQuestion(
        memoryTracker: number,
    ): CancelablePromise<RecallPrompt> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/recall-prompts/{memoryTracker}/random-question',
            path: {
                'memoryTracker': memoryTracker,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
