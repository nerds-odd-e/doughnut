/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Notebook } from '../models/Notebook';
import type { NotebookCertificateApproval } from '../models/NotebookCertificateApproval';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestNotebookCertificateApprovalControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param notebook
     * @returns Notebook OK
     * @throws ApiError
     */
    public requestNotebookApproval(
        notebook: number,
    ): CancelablePromise<Notebook> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/notebooks/{notebook}/request-approval',
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
    public approveNoteBook(
        notebook: number,
    ): CancelablePromise<Notebook> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/notebooks/{notebook}/approve',
            path: {
                'notebook': notebook,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @returns NotebookCertificateApproval OK
     * @throws ApiError
     */
    public getAllPendingRequestNotebooks(): CancelablePromise<Array<NotebookCertificateApproval>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/notebooks/getAllPendingRequestNoteBooks',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
