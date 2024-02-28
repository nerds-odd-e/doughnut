/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { LinkCreation } from '../models/LinkCreation';
import type { Note } from '../models/Note';
import type { NoteRealm } from '../models/NoteRealm';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestLinkControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param link
     * @param requestBody
     * @returns NoteRealm OK
     * @throws ApiError
     */
    public updateLink(
        link: Note,
        requestBody: LinkCreation,
    ): CancelablePromise<NoteRealm> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/links/{link}',
            path: {
                'link': link,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param link
     * @param perspective
     * @returns NoteRealm OK
     * @throws ApiError
     */
    public deleteLink(
        link: number,
        perspective: string,
    ): CancelablePromise<NoteRealm> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/links/{link}/{perspective}/delete',
            path: {
                'link': link,
                'perspective': perspective,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param sourceNote
     * @param targetNote
     * @param requestBody
     * @returns NoteRealm OK
     * @throws ApiError
     */
    public linkNoteFinalize(
        sourceNote: number,
        targetNote: number,
        requestBody: LinkCreation,
    ): CancelablePromise<NoteRealm> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/links/create/{sourceNote}/{targetNote}',
            path: {
                'sourceNote': sourceNote,
                'targetNote': targetNote,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
