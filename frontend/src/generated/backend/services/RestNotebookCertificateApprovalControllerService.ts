/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { NotebookCertificateApproval } from '../models/NotebookCertificateApproval';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestNotebookCertificateApprovalControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param notebookCertificateApproval
     * @returns NotebookCertificateApproval OK
     * @throws ApiError
     */
    public approve(
        notebookCertificateApproval: number,
    ): CancelablePromise<NotebookCertificateApproval> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/notebook_certificate_approvals/{notebookCertificateApproval}/approve',
            path: {
                'notebookCertificateApproval': notebookCertificateApproval,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param notebook
     * @returns NotebookCertificateApproval OK
     * @throws ApiError
     */
    public requestApprovalForNotebook(
        notebook: number,
    ): CancelablePromise<NotebookCertificateApproval> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/notebook_certificate_approvals/request-approval/{notebook}',
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
    public getAllPendingRequest(): CancelablePromise<Array<NotebookCertificateApproval>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/notebook_certificate_approvals/get-all-pending-request',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param notebook
     * @returns NotebookCertificateApproval OK
     * @throws ApiError
     */
    public getApprovalForNotebook(
        notebook: number,
    ): CancelablePromise<NotebookCertificateApproval> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/notebook_certificate_approvals/for-notebook/{notebook}',
            path: {
                'notebook': notebook,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
