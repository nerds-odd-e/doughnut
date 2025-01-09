/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestObsidianImportControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * Import Obsidian file
     * @param notebookId Notebook ID
     * @param formData
     * @returns any OK
     * @throws ApiError
     */
    public importObsidian(
        notebookId: number,
        formData?: {
            /**
             * Obsidian zip file to import
             */
            file: Blob;
        },
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/notebooks/{notebookId}/obsidian',
            path: {
                'notebookId': notebookId,
            },
            formData: formData,
            mediaType: 'multipart/form-data',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
