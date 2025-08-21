/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { NoteSearchResult } from '../models/NoteSearchResult';
import type { SearchTerm } from '../models/SearchTerm';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestSearchControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param note
     * @param requestBody
     * @returns NoteSearchResult OK
     * @throws ApiError
     */
    public searchForLinkTargetWithin(
        note: number,
        requestBody: SearchTerm,
    ): CancelablePromise<Array<NoteSearchResult>> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/notes/{note}/search',
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
     * @param requestBody
     * @returns NoteSearchResult OK
     * @throws ApiError
     */
    public searchForLinkTarget(
        requestBody: SearchTerm,
    ): CancelablePromise<Array<NoteSearchResult>> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/notes/search',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
