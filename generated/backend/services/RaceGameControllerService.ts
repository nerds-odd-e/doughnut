/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { RaceGameProgressDTO } from '../models/RaceGameProgressDTO';
import type { RaceGameRequestDTO } from '../models/RaceGameRequestDTO';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RaceGameControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param requestBody
     * @returns any OK
     * @throws ApiError
     */
    public resetGame(
        requestBody: RaceGameRequestDTO,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/race/reset',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param requestBody
     * @returns any OK
     * @throws ApiError
     */
    public rollDiceSuper(
        requestBody: RaceGameRequestDTO,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/race/go_super',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param requestBody
     * @returns any OK
     * @throws ApiError
     */
    public rollDiceNormal(
        requestBody: RaceGameRequestDTO,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/race/go_normal',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param playerId
     * @returns RaceGameProgressDTO OK
     * @throws ApiError
     */
    public getCurrentProgress(
        playerId: string,
    ): CancelablePromise<RaceGameProgressDTO> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/race/current_progress',
            query: {
                'playerId': playerId,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
