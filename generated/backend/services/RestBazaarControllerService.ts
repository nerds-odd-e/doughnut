/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BazaarNotebook } from '../models/BazaarNotebook';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestBazaarControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param bazaarNotebook
     * @returns BazaarNotebook OK
     * @throws ApiError
     */
    public removeFromBazaar(
        bazaarNotebook: number,
    ): CancelablePromise<Array<BazaarNotebook>> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/bazaar/{bazaarNotebook}/remove',
            path: {
                'bazaarNotebook': bazaarNotebook,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @returns BazaarNotebook OK
     * @throws ApiError
     */
    public bazaar(): CancelablePromise<Array<BazaarNotebook>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/bazaar',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
