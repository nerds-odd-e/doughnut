/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { DueMemoryTrackers } from '../models/DueMemoryTrackers';
import type { InitialInfo } from '../models/InitialInfo';
import type { MemoryTracker } from '../models/MemoryTracker';
import type { Note } from '../models/Note';
import type { ReviewStatus } from '../models/ReviewStatus';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestRecallsControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param requestBody
     * @returns MemoryTracker OK
     * @throws ApiError
     */
    public create(
        requestBody: InitialInfo,
    ): CancelablePromise<MemoryTracker> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/recalls',
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
     * @returns DueMemoryTrackers OK
     * @throws ApiError
     */
    public repeatReview(
        timezone: string,
        dueindays?: number,
    ): CancelablePromise<DueMemoryTrackers> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/recalls/repeat',
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
            url: '/api/recalls/overview',
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
            url: '/api/recalls/initial',
            query: {
                'timezone': timezone,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
