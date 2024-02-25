/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { NotebooksViewedByUser } from '../models/NotebooksViewedByUser';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestBazaarControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
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
