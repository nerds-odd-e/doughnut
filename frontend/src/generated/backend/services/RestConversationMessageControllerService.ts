/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Conversation } from '../models/Conversation';
import type { ConversationMessage } from '../models/ConversationMessage';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestConversationMessageControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param note
     * @param requestBody
     * @returns Conversation OK
     * @throws ApiError
     */
    public startConversationAboutNote(
        note: number,
        requestBody: string,
    ): CancelablePromise<Conversation> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/message/note/{note}',
            path: {
                'note': note,
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
     * @returns ConversationMessage OK
     * @throws ApiError
     */
    public replyToConversation(
        conversationId: number,
        requestBody: string,
    ): CancelablePromise<ConversationMessage> {
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
     * @param assessmentQuestion
     * @param requestBody
     * @returns Conversation OK
     * @throws ApiError
     */
    public startConversationAboutAssessmentQuestion(
        assessmentQuestion: number,
        requestBody: string,
    ): CancelablePromise<Conversation> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/message/assessment-question/{assessmentQuestion}',
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
     * @returns ConversationMessage OK
     * @throws ApiError
     */
    public markConversationAsRead(
        conversationId: number,
    ): CancelablePromise<Array<ConversationMessage>> {
        return this.httpRequest.request({
            method: 'PATCH',
            url: '/api/message/read/{conversationId}',
            path: {
                'conversationId': conversationId,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @returns ConversationMessage OK
     * @throws ApiError
     */
    public getUnreadConversations(): CancelablePromise<Array<ConversationMessage>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/message/unreadCount',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param conversationId
     * @returns ConversationMessage OK
     * @throws ApiError
     */
    public getConversationDetails(
        conversationId: number,
    ): CancelablePromise<Array<ConversationMessage>> {
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
    public getConversationsOfCurrentUser(): CancelablePromise<Array<Conversation>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/message/all',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
