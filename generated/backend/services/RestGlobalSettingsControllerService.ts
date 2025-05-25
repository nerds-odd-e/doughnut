/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { GlobalAiModelSettings } from '../models/GlobalAiModelSettings';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestGlobalSettingsControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @returns GlobalAiModelSettings OK
     * @throws ApiError
     */
    public getCurrentModelVersions(): CancelablePromise<GlobalAiModelSettings> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/settings/current-model-version',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param requestBody
     * @returns GlobalAiModelSettings OK
     * @throws ApiError
     */
    public setCurrentModelVersions(
        requestBody: GlobalAiModelSettings,
    ): CancelablePromise<GlobalAiModelSettings> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/settings/current-model-version',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
