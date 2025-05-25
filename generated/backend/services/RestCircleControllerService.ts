/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Circle } from '../models/Circle';
import type { CircleForUserView } from '../models/CircleForUserView';
import type { CircleJoiningByInvitation } from '../models/CircleJoiningByInvitation';
import type { NoteCreationDTO } from '../models/NoteCreationDTO';
import type { RedirectToNoteResponse } from '../models/RedirectToNoteResponse';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestCircleControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @returns Circle OK
     * @throws ApiError
     */
    public index(): CancelablePromise<Array<Circle>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/circles',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param requestBody
     * @returns Circle OK
     * @throws ApiError
     */
    public createCircle(
        requestBody: Circle,
    ): CancelablePromise<Circle> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/circles',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param circle
     * @param requestBody
     * @returns RedirectToNoteResponse OK
     * @throws ApiError
     */
    public createNotebookInCircle(
        circle: number,
        requestBody: NoteCreationDTO,
    ): CancelablePromise<RedirectToNoteResponse> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/circles/{circle}/notebooks',
            path: {
                'circle': circle,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param requestBody
     * @returns Circle OK
     * @throws ApiError
     */
    public joinCircle(
        requestBody: CircleJoiningByInvitation,
    ): CancelablePromise<Circle> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/circles/join',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param circle
     * @returns CircleForUserView OK
     * @throws ApiError
     */
    public showCircle(
        circle: number,
    ): CancelablePromise<CircleForUserView> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/circles/{circle}',
            path: {
                'circle': circle,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
