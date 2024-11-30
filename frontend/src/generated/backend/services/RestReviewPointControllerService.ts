/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { MemoryTracker } from '../models/MemoryTracker';
import type { SelfEvaluation } from '../models/SelfEvaluation';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestReviewPointControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param reviewPoint
     * @param requestBody
     * @returns MemoryTracker OK
     * @throws ApiError
     */
    public selfEvaluate(
        reviewPoint: number,
        requestBody: SelfEvaluation,
    ): CancelablePromise<MemoryTracker> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/memory-trackers/{reviewPoint}/self-evaluate',
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
     * @returns MemoryTracker OK
     * @throws ApiError
     */
    public removeFromRepeating(
        reviewPoint: number,
    ): CancelablePromise<MemoryTracker> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/memory-trackers/{reviewPoint}/remove',
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
     * @returns MemoryTracker OK
     * @throws ApiError
     */
    public markAsRepeated(
        reviewPoint: number,
        successful: boolean,
    ): CancelablePromise<MemoryTracker> {
        return this.httpRequest.request({
            method: 'PATCH',
            url: '/api/memory-trackers/{reviewPoint}/mark-as-repeated',
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
     * @returns MemoryTracker OK
     * @throws ApiError
     */
    public show1(
        reviewPoint: number,
    ): CancelablePromise<MemoryTracker> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/memory-trackers/{reviewPoint}',
            path: {
                'reviewPoint': reviewPoint,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @returns MemoryTracker OK
     * @throws ApiError
     */
    public getRecentlyReviewedPoints(): CancelablePromise<Array<MemoryTracker>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/memory-trackers/recently-reviewed',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @returns MemoryTracker OK
     * @throws ApiError
     */
    public getRecentReviewPoints(): CancelablePromise<Array<MemoryTracker>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/memory-trackers/recent',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
