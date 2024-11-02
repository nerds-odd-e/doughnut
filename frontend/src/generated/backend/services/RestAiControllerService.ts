/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AiAssistantResponse } from '../models/AiAssistantResponse';
import type { AiCompletionParams } from '../models/AiCompletionParams';
import type { AiGeneratedImage } from '../models/AiGeneratedImage';
import type { ChatRequest } from '../models/ChatRequest';
import type { DummyForGeneratingTypes } from '../models/DummyForGeneratingTypes';
import type { Message } from '../models/Message';
import type { NotebookAssistant } from '../models/NotebookAssistant';
import type { NotebookAssistantCreationParams } from '../models/NotebookAssistantCreationParams';
import type { SseEmitter } from '../models/SseEmitter';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestAiControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param note
     * @param requestBody
     * @returns AiAssistantResponse OK
     * @throws ApiError
     */
    public getCompletion(
        note: number,
        requestBody: AiCompletionParams,
    ): CancelablePromise<AiAssistantResponse> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/ai/{note}/completion',
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
     * @param notebook
     * @param requestBody
     * @returns NotebookAssistant OK
     * @throws ApiError
     */
    public recreateNotebookAssistant(
        notebook: number,
        requestBody: NotebookAssistantCreationParams,
    ): CancelablePromise<NotebookAssistant> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/ai/recreate-notebook-assistant/{notebook}',
            path: {
                'notebook': notebook,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @returns string OK
     * @throws ApiError
     */
    public recreateAllAssistants(): CancelablePromise<Record<string, string>> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/ai/recreate-all-assistants',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param requestBody
     * @returns AiGeneratedImage OK
     * @throws ApiError
     */
    public generateImage(
        requestBody: string,
    ): CancelablePromise<AiGeneratedImage> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/ai/generate-image',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param note
     * @returns Message OK
     * @throws ApiError
     */
    public tryRestoreChat(
        note: number,
    ): CancelablePromise<Array<Message>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/ai/chat/{note}',
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
     * @returns SseEmitter OK
     * @throws ApiError
     */
    public chat(
        note: number,
        requestBody: ChatRequest,
    ): CancelablePromise<SseEmitter> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/ai/chat/{note}',
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
     * @returns DummyForGeneratingTypes OK
     * @throws ApiError
     */
    public dummyEntryToGenerateDataTypesThatAreRequiredInEventStream(): CancelablePromise<DummyForGeneratingTypes> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/ai/dummy',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @returns string OK
     * @throws ApiError
     */
    public getAvailableGptModels(): CancelablePromise<Array<string>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/ai/available-gpt-models',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
