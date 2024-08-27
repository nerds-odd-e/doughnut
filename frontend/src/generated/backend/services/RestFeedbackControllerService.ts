/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { FeedbackDTO } from '../models/FeedbackDTO';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestFeedbackControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param requestBody
     * @returns string OK
     * @throws ApiError
     */
    public sendFeedback(
        requestBody: FeedbackDTO,
    ): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/feedback/sendFeedback',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
