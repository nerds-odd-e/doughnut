/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { NoteRealm } from '../models/NoteRealm';
import type { NoteUpdateDetailsDTO } from '../models/NoteUpdateDetailsDTO';
import type { NoteUpdateTopicDTO } from '../models/NoteUpdateTopicDTO';
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
    public updateNoteTopicConstructor(
        note: number,
        requestBody?: NoteUpdateTopicDTO,
    ): CancelablePromise<NoteRealm> {
        return this.httpRequest.request({
            method: 'PATCH',
            url: '/api/text_content/{note}/topic-constructor',
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
        requestBody?: NoteUpdateDetailsDTO,
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
