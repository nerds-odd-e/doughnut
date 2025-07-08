/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { NotebookAssistant } from '../models/NotebookAssistant';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestAiAssistantCreationControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param notebook
     * @returns NotebookAssistant OK
     * @throws ApiError
     */
    public recreateNotebookAssistant(
        notebook: number,
    ): CancelablePromise<NotebookAssistant> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/ai/assistant/recreate-notebook/{notebook}',
            path: {
                'notebook': notebook,
            },
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
