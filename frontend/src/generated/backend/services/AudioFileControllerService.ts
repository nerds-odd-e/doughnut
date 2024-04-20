/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AudioUploadDTO } from '../models/AudioUploadDTO';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class AudioFileControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param formData
     * @returns string OK
     * @throws ApiError
     */
    public uploadAudio(
        formData?: AudioUploadDTO,
    ): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/audio/{convert}',
            formData: formData,
            mediaType: 'multipart/form-data',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param audio
     * @returns string OK
     * @throws ApiError
     */
    public downloadAudio(
        audio: number,
    ): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/audio/{audio}',
            path: {
                'audio': audio,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
