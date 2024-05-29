/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AnsweredQuestion } from '../models/AnsweredQuestion';
import type { DueReviewPoints } from '../models/DueReviewPoints';
import type { InitialInfo } from '../models/InitialInfo';
import type { Note } from '../models/Note';
import type { ReviewPoint } from '../models/ReviewPoint';
import type { ReviewStatus } from '../models/ReviewStatus';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestReviewsControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param requestBody
     * @returns ReviewPoint OK
     * @throws ApiError
     */
    public create(
        requestBody: InitialInfo,
    ): CancelablePromise<ReviewPoint> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/reviews',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param timezone
     * @param dueindays
     * @returns DueReviewPoints OK
     * @throws ApiError
     */
    public repeatReview(
        timezone: string,
        dueindays?: number,
    ): CancelablePromise<DueReviewPoints> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/reviews/repeat',
            query: {
                'timezone': timezone,
                'dueindays': dueindays,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param timezone
     * @returns ReviewStatus OK
     * @throws ApiError
     */
    public overview(
        timezone: string,
    ): CancelablePromise<ReviewStatus> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/reviews/overview',
            query: {
                'timezone': timezone,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param timezone
     * @returns Note OK
     * @throws ApiError
     */
    public initialReview(
        timezone: string,
    ): CancelablePromise<Array<Note>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/reviews/initial',
            query: {
                'timezone': timezone,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param answer
     * @returns AnsweredQuestion OK
     * @throws ApiError
     */
    public showAnswer(
        answer: number,
    ): CancelablePromise<AnsweredQuestion> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/reviews/answers/{answer}',
            path: {
                'answer': answer,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
