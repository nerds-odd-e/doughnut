/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { User } from '../models/User';
import type { UserDTO } from '../models/UserDTO';
import type { UserTokenDTO } from '../models/UserTokenDTO';
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
    public createUser(
        requestBody: User,
    ): CancelablePromise<User> {
        return this.httpRequest.request({
            method: 'POST',
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
     * @returns UserTokenDTO OK
     * @throws ApiError
     */
    public createUserToken(
        user: number,
    ): CancelablePromise<UserTokenDTO> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/user/{user}/token',
            path: {
                'user': user,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param user
     * @returns any OK
     * @throws ApiError
     */
    public deleteUserToken(
        user: number,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/user/{user}/token',
            path: {
                'user': user,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param user
     * @param requestBody
     * @returns User OK
     * @throws ApiError
     */
    public updateUser(
        user: number,
        requestBody: UserDTO,
    ): CancelablePromise<User> {
        return this.httpRequest.request({
            method: 'PATCH',
            url: '/api/user/{user}',
            path: {
                'user': user,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param user
     * @returns UserTokenDTO OK
     * @throws ApiError
     */
    public getUserTokens(
        user: number,
    ): CancelablePromise<Array<UserTokenDTO>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/user/{user}/tokens',
            path: {
                'user': user,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
