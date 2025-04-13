/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ExportDataSchema } from '../models/ExportDataSchema';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class ExportControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param notebook
     * @returns ExportDataSchema OK
     * @throws ApiError
     */
    public exportNotebook(
        notebook: number,
    ): CancelablePromise<ExportDataSchema> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/export/notebooks/{notebook}',
            path: {
                'notebook': notebook,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @returns ExportDataSchema OK
     * @throws ApiError
     */
    public exportAllNotebooks(): CancelablePromise<ExportDataSchema> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/export/all',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
