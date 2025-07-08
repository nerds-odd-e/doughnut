/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { QuestionSuggestionParams } from '../models/QuestionSuggestionParams';
import type { SuggestedQuestionForFineTuning } from '../models/SuggestedQuestionForFineTuning';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestFineTuningDataControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param suggestedQuestion
     * @returns SuggestedQuestionForFineTuning OK
     * @throws ApiError
     */
    public duplicate(
        suggestedQuestion: number,
    ): CancelablePromise<SuggestedQuestionForFineTuning> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/fine-tuning/{suggestedQuestion}/duplicate',
            path: {
                'suggestedQuestion': suggestedQuestion,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param suggestedQuestion
     * @returns SuggestedQuestionForFineTuning OK
     * @throws ApiError
     */
    public delete(
        suggestedQuestion: number,
    ): CancelablePromise<SuggestedQuestionForFineTuning> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/fine-tuning/{suggestedQuestion}/delete',
            path: {
                'suggestedQuestion': suggestedQuestion,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @returns any OK
     * @throws ApiError
     */
    public uploadAndTriggerFineTuning(): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/fine-tuning/upload-and-trigger-fine-tuning',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param suggestedQuestion
     * @param requestBody
     * @returns SuggestedQuestionForFineTuning OK
     * @throws ApiError
     */
    public updateSuggestedQuestionForFineTuning(
        suggestedQuestion: number,
        requestBody: QuestionSuggestionParams,
    ): CancelablePromise<SuggestedQuestionForFineTuning> {
        return this.httpRequest.request({
            method: 'PATCH',
            url: '/api/fine-tuning/{suggestedQuestion}/update-suggested-question-for-fine-tuning',
            path: {
                'suggestedQuestion': suggestedQuestion,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @returns SuggestedQuestionForFineTuning OK
     * @throws ApiError
     */
    public getAllSuggestedQuestions(): CancelablePromise<Array<SuggestedQuestionForFineTuning>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/fine-tuning/all-suggested-questions-for-fine-tuning',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
