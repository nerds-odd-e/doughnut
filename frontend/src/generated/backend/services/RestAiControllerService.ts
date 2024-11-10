/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AiGeneratedImage } from '../models/AiGeneratedImage';
import type { DummyForGeneratingTypes } from '../models/DummyForGeneratingTypes';
import type { NotebookAssistant } from '../models/NotebookAssistant';
import type { NotebookAssistantCreationParams } from '../models/NotebookAssistantCreationParams';
import type { ToolCallResult } from '../models/ToolCallResult';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestAiControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param note
     * @returns string OK
     * @throws ApiError
     */
    public suggestTopicTitle(
        note: number,
    ): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/ai/suggest-topic-title/{note}',
            path: {
                'note': note,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param threadId
     * @param runId
     * @param toolCallId
     * @param requestBody
     * @returns any OK
     * @throws ApiError
     */
    public submitToolCallResult(
        threadId: string,
        runId: string,
        toolCallId: string,
        requestBody: ToolCallResult,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/ai/submit-tool-result/{threadId}/{runId}/{toolCallId}',
            path: {
                'threadId': threadId,
                'runId': runId,
                'toolCallId': toolCallId,
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
     * @param threadId
     * @param runId
     * @returns any OK
     * @throws ApiError
     */
    public cancelRun(
        threadId: string,
        runId: string,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/ai/cancel-run/{threadId}/{runId}',
            path: {
                'threadId': threadId,
                'runId': runId,
            },
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
