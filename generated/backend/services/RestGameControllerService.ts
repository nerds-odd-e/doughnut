/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Players } from '../models/Players';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestGameControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @returns Players OK
     * @throws ApiError
     */
    public joinGame(): CancelablePromise<Players> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/games/join',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
