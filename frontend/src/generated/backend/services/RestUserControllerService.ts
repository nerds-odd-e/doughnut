/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { User } from '../models/User';
import type { UserDTO } from '../models/UserDTO';
import type { UserToken } from '../models/UserToken';
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
     * @returns UserToken OK
     * @throws ApiError
     */
    public generateToken(): CancelablePromise<UserToken> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/user/generate-token',
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
     * @param mcpToken
     * @returns string OK
     * @throws ApiError
     */
    public getUserInfoByMcpToken(
        mcpToken: string,
    ): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/user/info',
            headers: {
                'mcpToken': mcpToken,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
