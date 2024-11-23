/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ReviewPoint } from '../models/ReviewPoint';
import type { SelfEvaluation } from '../models/SelfEvaluation';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestReviewPointControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param reviewPoint
     * @param requestBody
     * @returns ReviewPoint OK
     * @throws ApiError
     */
    public selfEvaluate(
        reviewPoint: number,
        requestBody: SelfEvaluation,
    ): CancelablePromise<ReviewPoint> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/review-points/{reviewPoint}/self-evaluate',
            path: {
                'reviewPoint': reviewPoint,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param reviewPoint
     * @returns ReviewPoint OK
     * @throws ApiError
     */
    public removeFromRepeating(
        reviewPoint: number,
    ): CancelablePromise<ReviewPoint> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/review-points/{reviewPoint}/remove',
            path: {
                'reviewPoint': reviewPoint,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param reviewPoint
     * @param successful
     * @returns ReviewPoint OK
     * @throws ApiError
     */
    public markAsRepeated(
        reviewPoint: number,
        successful: boolean,
    ): CancelablePromise<ReviewPoint> {
        return this.httpRequest.request({
            method: 'PATCH',
            url: '/api/review-points/{reviewPoint}/mark-as-repeated',
            path: {
                'reviewPoint': reviewPoint,
            },
            query: {
                'successful': successful,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param reviewPoint
     * @returns ReviewPoint OK
     * @throws ApiError
     */
    public show(
        reviewPoint: number,
    ): CancelablePromise<ReviewPoint> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/review-points/{reviewPoint}',
            path: {
                'reviewPoint': reviewPoint,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @returns ReviewPoint OK
     * @throws ApiError
     */
    public getRecentReviewPoints(): CancelablePromise<Array<ReviewPoint>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/review-points/recent',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
