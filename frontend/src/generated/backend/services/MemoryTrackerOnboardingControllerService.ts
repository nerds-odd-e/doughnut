/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { InitialInfo } from '../models/InitialInfo';
import type { MemoryTracker } from '../models/MemoryTracker';
import type { Note } from '../models/Note';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class MemoryTrackerOnboardingControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param requestBody
     * @returns MemoryTracker OK
     * @throws ApiError
     */
    public onboard(
        requestBody: InitialInfo,
    ): CancelablePromise<MemoryTracker> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/memory-tracker-onboarding',
            body: requestBody,
            mediaType: 'application/json',
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
    public onboarding(
        timezone: string,
    ): CancelablePromise<Array<Note>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/memory-tracker-onboarding/onboarding',
            query: {
                'timezone': timezone,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
