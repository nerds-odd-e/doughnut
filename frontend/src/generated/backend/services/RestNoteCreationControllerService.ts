/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { NoteCreationDTO } from '../models/NoteCreationDTO';
import type { NoteCreationRresult } from '../models/NoteCreationRresult';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestNoteCreationControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param referenceNote
     * @param requestBody
     * @returns NoteCreationRresult OK
     * @throws ApiError
     */
    public createNoteAfter(
        referenceNote: number,
        requestBody: NoteCreationDTO,
    ): CancelablePromise<NoteCreationRresult> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/notes/{referenceNote}/create-after',
            path: {
                'referenceNote': referenceNote,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param parentNote
     * @param requestBody
     * @returns NoteCreationRresult OK
     * @throws ApiError
     */
    public createNote(
        parentNote: number,
        requestBody: NoteCreationDTO,
    ): CancelablePromise<NoteCreationRresult> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/notes/{parentNote}/create',
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
