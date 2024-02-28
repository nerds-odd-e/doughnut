/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Notebook } from '../models/Notebook';
import type { NotebooksViewedByUser } from '../models/NotebooksViewedByUser';
import type { NoteCreationDTO } from '../models/NoteCreationDTO';
import type { RedirectToNoteResponse } from '../models/RedirectToNoteResponse';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestNotebookControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param requestBody
     * @returns Notebook OK
     * @throws ApiError
     */
    public update1(
        requestBody?: Notebook,
    ): CancelablePromise<Notebook> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/notebooks/{notebook}',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param notebook
     * @returns Notebook OK
     * @throws ApiError
     */
    public shareNotebook(
        notebook: number,
    ): CancelablePromise<Notebook> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/notebooks/{notebook}/share',
            path: {
                'notebook': notebook,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param requestBody
     * @returns RedirectToNoteResponse OK
     * @throws ApiError
     */
    public createNotebook(
        requestBody: NoteCreationDTO,
    ): CancelablePromise<RedirectToNoteResponse> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/notebooks/create',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @returns NotebooksViewedByUser OK
     * @throws ApiError
     */
    public myNotebooks(): CancelablePromise<NotebooksViewedByUser> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/notebooks',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
