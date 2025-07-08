/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { NotesTestData } from '../models/NotesTestData';
import type { PredefinedQuestion } from '../models/PredefinedQuestion';
import type { PredefinedQuestionsTestData } from '../models/PredefinedQuestionsTestData';
import type { Randomization } from '../models/Randomization';
import type { SuggestedQuestionsData } from '../models/SuggestedQuestionsData';
import type { TimeTravel } from '../models/TimeTravel';
import type { TimeTravelRelativeToNow } from '../models/TimeTravelRelativeToNow';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class TestabilityRestControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @returns string OK
     * @throws ApiError
     */
    public closeAllGithubIssues(): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/testability/use_real_sandbox_github_and_close_all_github_issues',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param requestBody
     * @returns string OK
     * @throws ApiError
     */
    public updateCurrentUser(
        requestBody: Record<string, string>,
    ): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/testability/update_current_user',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @returns string OK
     * @throws ApiError
     */
    public triggerException(): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/testability/trigger_exception',
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
    public timeTravelRelativeToNow(
        requestBody: TimeTravelRelativeToNow,
    ): CancelablePromise<Array<Record<string, any>>> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/testability/time_travel_relative_to_now',
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
    public timeTravel(
        requestBody: TimeTravel,
    ): CancelablePromise<Array<Record<string, any>>> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/testability/time_travel',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param requestBody
     * @returns string OK
     * @throws ApiError
     */
    public shareToBazaar(
        requestBody: Record<string, string>,
    ): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/testability/share_to_bazaar',
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
    public replaceServiceUrl(
        requestBody: Record<string, string>,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/testability/replace_service_url',
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
    public randomizer(
        requestBody: Randomization,
    ): CancelablePromise<Array<Record<string, any>>> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/testability/randomizer',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param requestBody
     * @returns string OK
     * @throws ApiError
     */
    public linkNotes(
        requestBody: Record<string, string>,
    ): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/testability/link_notes',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param requestBody
     * @returns string OK
     * @throws ApiError
     */
    public injectSuggestedQuestion(
        requestBody: SuggestedQuestionsData,
    ): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/testability/inject_suggested_questions',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param requestBody
     * @returns number OK
     * @throws ApiError
     */
    public injectNotes(
        requestBody: NotesTestData,
    ): CancelablePromise<Record<string, number>> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/testability/inject_notes',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param requestBody
     * @returns string OK
     * @throws ApiError
     */
    public injectCircle(
        requestBody: Record<string, string>,
    ): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/testability/inject_circle',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param requestBody
     * @returns PredefinedQuestion OK
     * @throws ApiError
     */
    public injectPredefinedQuestion(
        requestBody: PredefinedQuestionsTestData,
    ): CancelablePromise<Array<PredefinedQuestion>> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/testability/inject-predefined-questions',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @returns boolean OK
     * @throws ApiError
     */
    public getFeatureToggle(): CancelablePromise<boolean> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/testability/feature_toggle',
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
    public enableFeatureToggle(
        requestBody: Record<string, string>,
    ): CancelablePromise<Array<Record<string, any>>> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/testability/feature_toggle',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @returns string OK
     * @throws ApiError
     */
    public resetDbAndTestabilitySettings(): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/testability/clean_db_and_reset_testability_settings',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @returns any OK
     * @throws ApiError
     */
    public githubIssues(): CancelablePromise<Array<Record<string, Record<string, any>>>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/testability/github_issues',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
