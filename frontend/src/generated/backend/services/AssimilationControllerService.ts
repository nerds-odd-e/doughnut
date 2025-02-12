/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AssimilationCountDTO } from '../models/AssimilationCountDTO';
import type { InitialInfo } from '../models/InitialInfo';
import type { MemoryTracker } from '../models/MemoryTracker';
import type { Note } from '../models/Note';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class AssimilationControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param requestBody
     * @returns MemoryTracker OK
     * @throws ApiError
     */
    public assimilate(
        requestBody: InitialInfo,
    ): CancelablePromise<Array<MemoryTracker>> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/assimilation',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param timezone
     * @returns AssimilationCountDTO OK
     * @throws ApiError
     */
    public getAssimilationCount(
        timezone: string,
    ): CancelablePromise<AssimilationCountDTO> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/assimilation/count',
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
    public assimilating(
        timezone: string,
    ): CancelablePromise<Array<Note>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/assimilation/assimilating',
            query: {
                'timezone': timezone,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
