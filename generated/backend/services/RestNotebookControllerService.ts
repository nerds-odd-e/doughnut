/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BareNote } from '../models/BareNote';
import type { Note } from '../models/Note';
import type { Notebook } from '../models/Notebook';
import type { NotebookAiAssistant } from '../models/NotebookAiAssistant';
import type { NotebookSettings } from '../models/NotebookSettings';
import type { NotebooksViewedByUser } from '../models/NotebooksViewedByUser';
import type { NoteCreationDTO } from '../models/NoteCreationDTO';
import type { RedirectToNoteResponse } from '../models/RedirectToNoteResponse';
import type { UpdateAiAssistantRequest } from '../models/UpdateAiAssistantRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestNotebookControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param notebook
     * @returns Notebook OK
     * @throws ApiError
     */
    public get(
        notebook: number,
    ): CancelablePromise<Notebook> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/notebooks/{notebook}',
            path: {
                'notebook': notebook,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param notebook
     * @param requestBody
     * @returns Notebook OK
     * @throws ApiError
     */
    public update1(
        notebook: number,
        requestBody: NotebookSettings,
    ): CancelablePromise<Notebook> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/notebooks/{notebook}',
            path: {
                'notebook': notebook,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param notebook
     * @returns any OK
     * @throws ApiError
     */
    public updateNotebookIndex(
        notebook: number,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/notebooks/{notebook}/update-index',
            path: {
                'notebook': notebook,
            },
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
     * @param notebook
     * @returns any OK
     * @throws ApiError
     */
    public resetNotebookIndex(
        notebook: number,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/notebooks/{notebook}/reset-index',
            path: {
                'notebook': notebook,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * Import Obsidian file
     * @param notebookId Notebook ID
     * @param formData
     * @returns any OK
     * @throws ApiError
     */
    public importObsidian(
        notebookId: number,
        formData?: {
            /**
             * Obsidian zip file to import
             */
            file: Blob;
        },
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/notebooks/{notebookId}/obsidian',
            path: {
                'notebookId': notebookId,
            },
            formData: formData,
            mediaType: 'multipart/form-data',
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
     * @param notebook
     * @param circle
     * @returns Notebook OK
     * @throws ApiError
     */
    public moveToCircle(
        notebook: number,
        circle: number,
    ): CancelablePromise<Notebook> {
        return this.httpRequest.request({
            method: 'PATCH',
            url: '/api/notebooks/{notebook}/move-to-circle/{circle}',
            path: {
                'notebook': notebook,
                'circle': circle,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param notebook
     * @returns NotebookAiAssistant OK
     * @throws ApiError
     */
    public getAiAssistant(
        notebook: number,
    ): CancelablePromise<NotebookAiAssistant> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/notebooks/{notebook}/ai-assistant',
            path: {
                'notebook': notebook,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param notebook
     * @param requestBody
     * @returns NotebookAiAssistant OK
     * @throws ApiError
     */
    public updateAiAssistant(
        notebook: number,
        requestBody: UpdateAiAssistantRequest,
    ): CancelablePromise<NotebookAiAssistant> {
        return this.httpRequest.request({
            method: 'PATCH',
            url: '/api/notebooks/{notebook}/ai-assistant',
            path: {
                'notebook': notebook,
            },
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
    /**
     * @param notebook
     * @returns string OK
     * @throws ApiError
     */
    public downloadNotebookForObsidian(
        notebook: number,
    ): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/notebooks/{notebook}/obsidian',
            path: {
                'notebook': notebook,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param notebook
     * @returns Note OK
     * @throws ApiError
     */
    public getNotes(
        notebook: number,
    ): CancelablePromise<Array<Note>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/notebooks/{notebook}/notes',
            path: {
                'notebook': notebook,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param notebook
     * @returns BareNote OK
     * @throws ApiError
     */
    public downloadNotebookDump(
        notebook: number,
    ): CancelablePromise<Array<BareNote>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/notebooks/{notebook}/dump',
            path: {
                'notebook': notebook,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
