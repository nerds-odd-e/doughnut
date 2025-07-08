/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestHealthCheckControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @returns string OK
     * @throws ApiError
     */
    public ping(): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/healthcheck',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @returns any OK
     * @throws ApiError
     */
    public dataUpgrade(): CancelablePromise<Array<Record<string, any>>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/data_upgrade',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
