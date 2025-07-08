/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AudioUploadDTO } from '../models/AudioUploadDTO';
import type { TextFromAudioWithCallInfo } from '../models/TextFromAudioWithCallInfo';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestAiAudioControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param formData
     * @returns TextFromAudioWithCallInfo OK
     * @throws ApiError
     */
    public audioToText(
        formData?: AudioUploadDTO,
    ): CancelablePromise<TextFromAudioWithCallInfo> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/audio/audio-to-text',
            formData: formData,
            mediaType: 'multipart/form-data',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
