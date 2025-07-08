/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Certificate } from '../models/Certificate';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestCertificateControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param notebook
     * @returns Certificate OK
     * @throws ApiError
     */
    public getCertificate(
        notebook: number,
    ): CancelablePromise<Certificate> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/certificate/{notebook}',
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
     * @returns Certificate OK
     * @throws ApiError
     */
    public claimCertificate(
        notebook: number,
    ): CancelablePromise<Certificate> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/certificate/{notebook}',
            path: {
                'notebook': notebook,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
