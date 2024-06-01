/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { NotebooksViewedByUser } from '../models/NotebooksViewedByUser';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestBazaarControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param notebook
     * @returns NotebooksViewedByUser OK
     * @throws ApiError
     */
    public removeFromBazaar(
        notebook: number,
    ): CancelablePromise<NotebooksViewedByUser> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/bazaar/{notebook}/remove',
            path: {
                'notebook': notebook,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @returns NotebooksViewedByUser OK
     * @throws ApiError
     */
    public bazaar(): CancelablePromise<NotebooksViewedByUser> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/bazaar',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
