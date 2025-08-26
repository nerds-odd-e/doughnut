/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { NoteCreationDTO } from '../models/NoteCreationDTO';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class McpNoteCreationControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param parentNote
     * @param requestBody
     * @returns string OK
     * @throws ApiError
     */
    public createNote1(
        parentNote: number,
        requestBody: NoteCreationDTO,
    ): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/mcp/notes/{parentNote}/create',
            path: {
                'parentNote': parentNote,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
