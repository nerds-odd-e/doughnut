/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { QuizQuestion1 } from '../models/QuizQuestion1';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestAssessmentControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param notebook
     * @returns QuizQuestion1 OK
     * @throws ApiError
     */
    public generateAssessmentQuestions(
        notebook: number,
    ): CancelablePromise<Array<QuizQuestion1>> {
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
