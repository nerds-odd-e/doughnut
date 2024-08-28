/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Conversation } from '../models/Conversation';
import type { FeedbackDTO } from '../models/FeedbackDTO';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestFeedbackControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param question
     * @param requestBody
     * @returns string OK
     * @throws ApiError
     */
    public sendFeedback(
        question: number,
        requestBody: FeedbackDTO,
    ): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/feedback/send/{question}',
            path: {
                'question': question,
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
    public getFeedback(): CancelablePromise<Array<Conversation>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/feedback',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
