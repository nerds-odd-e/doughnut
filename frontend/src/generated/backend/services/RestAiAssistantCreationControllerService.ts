/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { NotebookAssistant } from '../models/NotebookAssistant';
import type { NotebookAssistantCreationParams } from '../models/NotebookAssistantCreationParams';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestAiAssistantCreationControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
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
            url: '/api/ai/assistant/recreate-notebook/{notebook}',
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
    public recreateDefaultAssistant(): CancelablePromise<Record<string, string>> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/ai/assistant/recreate-default',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
