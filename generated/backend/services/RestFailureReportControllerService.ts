/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { FailureReportForView } from '../models/FailureReportForView';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestFailureReportControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @returns any OK
     * @throws ApiError
     */
    public failureReports(): CancelablePromise<Record<string, any>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/failure-reports',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param failureReport
     * @returns FailureReportForView OK
     * @throws ApiError
     */
    public show2(
        failureReport: number,
    ): CancelablePromise<FailureReportForView> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/failure-reports/{failureReport}',
            path: {
                'failureReport': failureReport,
            },
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
    public deleteFailureReports(
        requestBody: Array<number>,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/failure-reports/delete',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
