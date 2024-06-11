/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { QuizQuestion } from '../models/QuizQuestion';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestAssessmentControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param notebook
     * @returns number OK
     * @throws ApiError
     */
    public countQuizQuestions(
        notebook: number,
    ): CancelablePromise<number> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/assessment/questions_count/{notebook}',
            path: {
                'notebook': notebook,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param notebook
     * @returns QuizQuestion OK
     * @throws ApiError
     */
    public generateAssessmentQuestions(
        notebook: number,
    ): CancelablePromise<Array<QuizQuestion>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/assessment/questions/{notebook}',
            path: {
                'notebook': notebook,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
