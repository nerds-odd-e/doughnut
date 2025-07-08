/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Subscription } from '../models/Subscription';
import type { SubscriptionDTO } from '../models/SubscriptionDTO';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestSubscriptionControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param subscription
     * @param requestBody
     * @returns Subscription OK
     * @throws ApiError
     */
    public update(
        subscription: number,
        requestBody: SubscriptionDTO,
    ): CancelablePromise<Subscription> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/subscriptions/{subscription}',
            path: {
                'subscription': subscription,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @returns number OK
     * @throws ApiError
     */
    public destroySubscription(): CancelablePromise<Array<number>> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/subscriptions/{subscription}/delete',
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
        notebook: number,
        requestBody: SubscriptionDTO,
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
