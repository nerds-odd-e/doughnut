/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestGameControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @returns number OK
     * @throws ApiError
     */
    public joinGame(): CancelablePromise<number> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/games/join',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
