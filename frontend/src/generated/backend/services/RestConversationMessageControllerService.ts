/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Conversation } from '../models/Conversation';
import type { ConversationDetail } from '../models/ConversationDetail';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestConversationMessageControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param assessmentQuestion
     * @param requestBody
     * @returns Conversation OK
     * @throws ApiError
     */
    public sendFeedback(
        assessmentQuestion: number,
        requestBody: string,
    ): CancelablePromise<Conversation> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/message/send/{assessmentQuestion}',
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
     * @param conversationId
     * @param requestBody
     * @returns ConversationDetail OK
     * @throws ApiError
     */
    public sendMessage(
        conversationId: number,
        requestBody: string,
    ): CancelablePromise<ConversationDetail> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/message/detail/send/{conversationId}',
            path: {
                'conversationId': conversationId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param conversationId
     * @returns ConversationDetail OK
     * @throws ApiError
     */
    public getMessageThreadsForConversation(
        conversationId: number,
    ): CancelablePromise<Array<ConversationDetail>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/message/detail/all/{conversationId}',
            path: {
                'conversationId': conversationId,
            },
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
            url: '/api/message/all',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
