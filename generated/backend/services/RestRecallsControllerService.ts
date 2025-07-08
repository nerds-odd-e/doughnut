/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { DueMemoryTrackers } from '../models/DueMemoryTrackers';
import type { RecallStatus } from '../models/RecallStatus';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestRecallsControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param timezone
     * @param dueindays
     * @returns DueMemoryTrackers OK
     * @throws ApiError
     */
    public recalling(
        timezone: string,
        dueindays?: number,
    ): CancelablePromise<DueMemoryTrackers> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/recalls/recalling',
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
     * @returns RecallStatus OK
     * @throws ApiError
     */
    public overview(
        timezone: string,
    ): CancelablePromise<RecallStatus> {
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
}
