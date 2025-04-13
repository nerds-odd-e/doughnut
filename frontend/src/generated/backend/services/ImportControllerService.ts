/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ImportDataSchema } from '../models/ImportDataSchema';
import type { ImportResult } from '../models/ImportResult';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class ImportControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param requestBody
     * @returns ImportResult OK
     * @throws ApiError
     */
    public importNotebook(
        requestBody: ImportDataSchema,
    ): CancelablePromise<ImportResult> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/import/notebooks',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param requestBody
     * @returns ImportResult OK
     * @throws ApiError
     */
    public importAllNotebooks(
        requestBody: ImportDataSchema,
    ): CancelablePromise<ImportResult> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/import/all',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
