/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { PredefinedQuestion } from '../models/PredefinedQuestion';
import type { QuestionSuggestionCreationParams } from '../models/QuestionSuggestionCreationParams';
import type { SuggestedQuestionForFineTuning } from '../models/SuggestedQuestionForFineTuning';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestPredefinedQuestionControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param predefinedQuestion
     * @returns PredefinedQuestion OK
     * @throws ApiError
     */
    public toggleApproval(
        predefinedQuestion: number,
    ): CancelablePromise<PredefinedQuestion> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/predefined-questions/{predefinedQuestion}/toggle-approval',
            path: {
                'predefinedQuestion': predefinedQuestion,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param predefinedQuestion
     * @param requestBody
     * @returns SuggestedQuestionForFineTuning OK
     * @throws ApiError
     */
    public suggestQuestionForFineTuning(
        predefinedQuestion: number,
        requestBody: QuestionSuggestionCreationParams,
    ): CancelablePromise<SuggestedQuestionForFineTuning> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/predefined-questions/{predefinedQuestion}/suggest-fine-tuning',
            path: {
                'predefinedQuestion': predefinedQuestion,
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
            url: '/api/predefined-questions/{note}/refine-question',
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
            url: '/api/predefined-questions/{note}/note-questions',
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
            url: '/api/predefined-questions/{note}/note-questions',
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
    public generateQuestionWithoutSave(
        note: number,
    ): CancelablePromise<PredefinedQuestion> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/predefined-questions/generate-question-without-save',
            query: {
                'note': note,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param predefinedQuestion
     * @returns void
     * @throws ApiError
     */
    public deleteQuestion(
        predefinedQuestion: number,
    ): CancelablePromise<void> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/predefined-questions/{predefinedQuestion}',
            path: {
                'predefinedQuestion': predefinedQuestion,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
