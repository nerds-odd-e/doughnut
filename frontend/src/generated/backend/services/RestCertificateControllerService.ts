/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Certificate } from '../models/Certificate';
import type { SaveCertificateDetails } from '../models/SaveCertificateDetails';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestCertificateControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param requestBody
     * @returns Certificate OK
     * @throws ApiError
     */
    public saveCertificate(
        requestBody: SaveCertificateDetails,
    ): CancelablePromise<Certificate> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/certificate',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
