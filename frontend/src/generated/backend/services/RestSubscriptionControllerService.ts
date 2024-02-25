/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Notebook } from '../models/Notebook';
import type { Subscription } from '../models/Subscription';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestSubscriptionControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param requestBody
     * @returns Subscription OK
     * @throws ApiError
     */
    public update(
        requestBody?: Subscription,
    ): CancelablePromise<Subscription> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/subscriptions/{subscription}',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param requestBody
     * @returns number OK
     * @throws ApiError
     */
    public destroySubscription(
        requestBody?: Subscription,
    ): CancelablePromise<Array<number>> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/subscriptions/{subscription}/delete',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param notebook
     * @param requestBody
     * @returns Subscription OK
     * @throws ApiError
     */
    public createSubscription(
        notebook: Notebook,
        requestBody?: Subscription,
    ): CancelablePromise<Subscription> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/subscriptions/notebooks/{notebook}/subscribe',
            path: {
                'notebook': notebook,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
