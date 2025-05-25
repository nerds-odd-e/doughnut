/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { NoteRealm } from '../models/NoteRealm';
import type { NoteUpdateDetailsDTO } from '../models/NoteUpdateDetailsDTO';
import type { NoteUpdateTitleDTO } from '../models/NoteUpdateTitleDTO';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestTextContentControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param note
     * @param requestBody
     * @returns NoteRealm OK
     * @throws ApiError
     */
    public updateNoteTitle(
        note: number,
        requestBody: NoteUpdateTitleDTO,
    ): CancelablePromise<NoteRealm> {
        return this.httpRequest.request({
            method: 'PATCH',
            url: '/api/text_content/{note}/title',
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
     * @param note
     * @param requestBody
     * @returns NoteRealm OK
     * @throws ApiError
     */
    public updateNoteDetails(
        note: number,
        requestBody: NoteUpdateDetailsDTO,
    ): CancelablePromise<NoteRealm> {
        return this.httpRequest.request({
            method: 'PATCH',
            url: '/api/text_content/{note}/details',
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
}
