/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Players } from '../models/Players';
import type { Rounds } from '../models/Rounds';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestGameControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @returns Players OK
     * @throws ApiError
     */
    public joinGame(): CancelablePromise<Players> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/games/join',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param id
     * @param mode
     * @returns Rounds OK
     * @throws ApiError
     */
    public rollDice(
        id: number,
        mode: string,
    ): CancelablePromise<Rounds> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/games/dice/{id}',
            query: {
                'id': id,
                'mode': mode,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @returns Players OK
     * @throws ApiError
     */
    public fetchPlayers(): CancelablePromise<Array<Players>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/games/fetch',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
