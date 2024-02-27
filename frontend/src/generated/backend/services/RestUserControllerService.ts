/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { User } from '../models/User';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestUserControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @returns User OK
     * @throws ApiError
     */
    public getUserProfile(): CancelablePromise<User> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/user',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param requestBody
     * @returns User OK
     * @throws ApiError
     */
    public updateUser(
        requestBody: User,
    ): CancelablePromise<User> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/user',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param user
     * @returns User OK
     * @throws ApiError
     */
    public createUser(
        user: User,
    ): CancelablePromise<User> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/user',
            query: {
                'user': user,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
