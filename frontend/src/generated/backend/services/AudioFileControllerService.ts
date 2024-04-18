/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class AudioFileControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param convert
     * @param requestBody
     * @returns string OK
     * @throws ApiError
     */
    public upload(
        convert: boolean,
        requestBody: {
            audioFile?: Blob;
        },
    ): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/audio/{convert}',
            path: {
                'convert': convert,
            },
            body: requestBody,
            mediaType: 'application/json',
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
            url: '/audio/{audio}',
            path: {
                'audio': audio,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
