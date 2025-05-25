/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { LinkCreation } from '../models/LinkCreation';
import type { NoteMoveDTO } from '../models/NoteMoveDTO';
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
        link: number,
        requestBody: LinkCreation,
    ): CancelablePromise<Array<NoteRealm>> {
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
     * @param sourceNote
     * @param targetNote
     * @param requestBody
     * @returns NoteRealm OK
     * @throws ApiError
     */
    public moveNote(
        sourceNote: number,
        targetNote: number,
        requestBody: NoteMoveDTO,
    ): CancelablePromise<Array<NoteRealm>> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/links/move/{sourceNote}/{targetNote}',
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
    ): CancelablePromise<Array<NoteRealm>> {
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
