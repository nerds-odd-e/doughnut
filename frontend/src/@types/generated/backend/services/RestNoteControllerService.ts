/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Note } from '../models/Note';
import type { NoteAccessories } from '../models/NoteAccessories';
import type { NoteCreationDTO } from '../models/NoteCreationDTO';
import type { NoteInfo } from '../models/NoteInfo';
import type { NotePositionViewedByUser } from '../models/NotePositionViewedByUser';
import type { NoteRealm } from '../models/NoteRealm';
import type { RedirectToNoteResponse } from '../models/RedirectToNoteResponse';
import type { ReviewSetting } from '../models/ReviewSetting';
import type { SearchTerm } from '../models/SearchTerm';
import type { WikidataAssociationCreation } from '../models/WikidataAssociationCreation';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestNoteControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param parentNote
     * @param requestBody
     * @returns NoteRealm OK
     * @throws ApiError
     */
    public createNote(
        parentNote: Note,
        requestBody?: NoteCreationDTO,
    ): CancelablePromise<NoteRealm> {
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
    /**
     * @param note
     * @param requestBody
     * @returns NoteRealm OK
     * @throws ApiError
     */
    public updateWikidataId(
        note: Note,
        requestBody: WikidataAssociationCreation,
    ): CancelablePromise<NoteRealm> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/notes/{note}/updateWikidataId',
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
     * @returns Note OK
     * @throws ApiError
     */
    public searchForLinkTargetWithin(
        note: Note,
        requestBody: SearchTerm,
    ): CancelablePromise<Array<Note>> {
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
     * @param note
     * @param requestBody
     * @returns RedirectToNoteResponse OK
     * @throws ApiError
     */
    public updateReviewSetting(
        note: Note,
        requestBody: ReviewSetting,
    ): CancelablePromise<RedirectToNoteResponse> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/notes/{note}/review-setting',
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
     * @returns NoteRealm OK
     * @throws ApiError
     */
    public deleteNote(
        note: Note,
    ): CancelablePromise<Array<NoteRealm>> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/notes/{note}/delete',
            path: {
                'note': note,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param requestBody
     * @returns Note OK
     * @throws ApiError
     */
    public searchForLinkTarget(
        requestBody: SearchTerm,
    ): CancelablePromise<Array<Note>> {
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
    /**
     * @param note
     * @returns NoteRealm OK
     * @throws ApiError
     */
    public show1(
        note: Note,
    ): CancelablePromise<NoteRealm> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/notes/{note}',
            path: {
                'note': note,
            },
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
    public updateNote(
        note: Note,
        requestBody?: NoteAccessories,
    ): CancelablePromise<NoteRealm> {
        return this.httpRequest.request({
            method: 'PATCH',
            url: '/api/notes/{note}',
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
     * @returns NoteRealm OK
     * @throws ApiError
     */
    public undoDeleteNote(
        note: Note,
    ): CancelablePromise<NoteRealm> {
        return this.httpRequest.request({
            method: 'PATCH',
            url: '/api/notes/{note}/undo-delete',
            path: {
                'note': note,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param note
     * @returns NotePositionViewedByUser OK
     * @throws ApiError
     */
    public getPosition(
        note: Note,
    ): CancelablePromise<NotePositionViewedByUser> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/notes/{note}/position',
            query: {
                'note': note,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param note
     * @returns NoteInfo OK
     * @throws ApiError
     */
    public getNoteInfo(
        note: Note,
    ): CancelablePromise<NoteInfo> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/notes/{note}/note-info',
            path: {
                'note': note,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
