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
     * @returns QuizQuestion OK
     * @throws ApiError
     */
    public generateAiQuestions(
        notebook: number,
    ): CancelablePromise<Array<QuizQuestion>> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/assessment/ai-questions/{notebook}',
            path: {
                'notebook': notebook,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
