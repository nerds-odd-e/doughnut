/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CurrentUserInfo } from '../models/CurrentUserInfo';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestCurrentUserInfoControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @returns CurrentUserInfo OK
     * @throws ApiError
     */
    public currentUserInfo(): CancelablePromise<CurrentUserInfo> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/user/current-user-info',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
