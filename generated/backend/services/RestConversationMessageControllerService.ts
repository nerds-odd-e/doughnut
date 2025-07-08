/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Conversation } from '../models/Conversation';
import type { ConversationMessage } from '../models/ConversationMessage';
import type { SseEmitter } from '../models/SseEmitter';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestConversationMessageControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
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
            url: '/api/conversation/{conversationId}/send',
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
     * @returns SseEmitter OK
     * @throws ApiError
     */
    public getAiReply(
        conversationId: number,
    ): CancelablePromise<SseEmitter> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/conversation/{conversationId}/ai-reply',
            path: {
                'conversationId': conversationId,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param recallPrompt
     * @returns Conversation OK
     * @throws ApiError
     */
    public startConversationAboutRecallPrompt(
        recallPrompt: number,
    ): CancelablePromise<Conversation> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/conversation/recall-prompt/{recallPrompt}',
            path: {
                'recallPrompt': recallPrompt,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param note
     * @returns Conversation OK
     * @throws ApiError
     */
    public getConversationsAboutNote(
        note: number,
    ): CancelablePromise<Array<Conversation>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/conversation/note/{note}',
            path: {
                'note': note,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
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
            url: '/api/conversation/note/{note}',
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
            url: '/api/conversation/assessment-question/{assessmentQuestion}',
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
            url: '/api/conversation/{conversationId}/read',
            path: {
                'conversationId': conversationId,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param conversationId
     * @returns Conversation OK
     * @throws ApiError
     */
    public getConversation(
        conversationId: number,
    ): CancelablePromise<Conversation> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/conversation/{conversationId}',
            path: {
                'conversationId': conversationId,
            },
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
    public getConversationMessages(
        conversationId: number,
    ): CancelablePromise<Array<ConversationMessage>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/conversation/{conversationId}/messages',
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
            url: '/api/conversation/unread',
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
            url: '/api/conversation/all',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
