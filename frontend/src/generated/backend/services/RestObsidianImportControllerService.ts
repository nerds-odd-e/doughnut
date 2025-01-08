/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { NoteRealm } from '../models/NoteRealm';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestObsidianImportControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * Import Obsidian file
     * @param parentNoteId Parent note ID
     * @param formData
     * @returns NoteRealm OK
     * @throws ApiError
     */
    public importObsidian(
        parentNoteId: number,
        formData?: {
            /**
             * Obsidian zip file to import
             */
            file: Blob;
        },
    ): CancelablePromise<NoteRealm> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/obsidian/{parentNoteId}/import',
            path: {
                'parentNoteId': parentNoteId,
            },
            formData: formData,
            mediaType: 'multipart/form-data',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
