/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AiGeneratedImage } from '../models/AiGeneratedImage';
import type { DummyForGeneratingTypes } from '../models/DummyForGeneratingTypes';
import type { SuggestedTitleDTO } from '../models/SuggestedTitleDTO';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestAiControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param note
     * @returns SuggestedTitleDTO OK
     * @throws ApiError
     */
    public suggestTitle(
        note: number,
    ): CancelablePromise<SuggestedTitleDTO> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/ai/suggest-title/{note}',
            path: {
                'note': note,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param requestBody
     * @returns AiGeneratedImage OK
     * @throws ApiError
     */
    public generateImage(
        requestBody: string,
    ): CancelablePromise<AiGeneratedImage> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/ai/generate-image',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @returns DummyForGeneratingTypes OK
     * @throws ApiError
     */
    public dummyEntryToGenerateDataTypesThatAreRequiredInEventStream(): CancelablePromise<DummyForGeneratingTypes> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/ai/dummy',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @returns string OK
     * @throws ApiError
     */
    public getAvailableGptModels(): CancelablePromise<Array<string>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/ai/available-gpt-models',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
