/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Conversation } from '../models/Conversation';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestFeedbackControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param assessmentQuestion
     * @param requestBody
     * @returns string OK
     * @throws ApiError
     */
    public sendFeedback(
        assessmentQuestion: number,
        requestBody: string,
    ): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/feedback/send/{assessmentQuestion}',
            path: {
                'assessmentQuestion': assessmentQuestion,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @returns Conversation OK
     * @throws ApiError
     */
    public getFeedbackThreadsForUser(): CancelablePromise<Array<Conversation>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/feedback/all',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
