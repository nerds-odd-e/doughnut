/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BaseHttpRequest } from './core/BaseHttpRequest';
import type { OpenAPIConfig } from './core/OpenAPI';
import { FetchHttpRequest } from './core/FetchHttpRequest';
import { RestAiControllerService } from './services/RestAiControllerService';
import { RestBazaarControllerService } from './services/RestBazaarControllerService';
import { RestCircleControllerService } from './services/RestCircleControllerService';
import { RestCurrentUserInfoControllerService } from './services/RestCurrentUserInfoControllerService';
import { RestFailureReportControllerService } from './services/RestFailureReportControllerService';
import { RestFineTuningDataControllerService } from './services/RestFineTuningDataControllerService';
import { RestGlobalSettingsControllerService } from './services/RestGlobalSettingsControllerService';
import { RestHealthCheckControllerService } from './services/RestHealthCheckControllerService';
import { RestLinkControllerService } from './services/RestLinkControllerService';
import { RestNotebookControllerService } from './services/RestNotebookControllerService';
import { RestNoteControllerService } from './services/RestNoteControllerService';
import { RestQuizQuestionControllerService } from './services/RestQuizQuestionControllerService';
import { RestReviewPointControllerService } from './services/RestReviewPointControllerService';
import { RestReviewsControllerService } from './services/RestReviewsControllerService';
import { RestSubscriptionControllerService } from './services/RestSubscriptionControllerService';
import { RestTextContentControllerService } from './services/RestTextContentControllerService';
import { RestUserControllerService } from './services/RestUserControllerService';
import { RestWikidataControllerService } from './services/RestWikidataControllerService';
import { TestabilityRestControllerService } from './services/TestabilityRestControllerService';
type HttpRequestConstructor = new (config: OpenAPIConfig) => BaseHttpRequest;
export class DoughnutApi {
    public readonly restAiController: RestAiControllerService;
    public readonly restBazaarController: RestBazaarControllerService;
    public readonly restCircleController: RestCircleControllerService;
    public readonly restCurrentUserInfoController: RestCurrentUserInfoControllerService;
    public readonly restFailureReportController: RestFailureReportControllerService;
    public readonly restFineTuningDataController: RestFineTuningDataControllerService;
    public readonly restGlobalSettingsController: RestGlobalSettingsControllerService;
    public readonly restHealthCheckController: RestHealthCheckControllerService;
    public readonly restLinkController: RestLinkControllerService;
    public readonly restNotebookController: RestNotebookControllerService;
    public readonly restNoteController: RestNoteControllerService;
    public readonly restQuizQuestionController: RestQuizQuestionControllerService;
    public readonly restReviewPointController: RestReviewPointControllerService;
    public readonly restReviewsController: RestReviewsControllerService;
    public readonly restSubscriptionController: RestSubscriptionControllerService;
    public readonly restTextContentController: RestTextContentControllerService;
    public readonly restUserController: RestUserControllerService;
    public readonly restWikidataController: RestWikidataControllerService;
    public readonly testabilityRestController: TestabilityRestControllerService;
    public readonly request: BaseHttpRequest;
    constructor(config?: Partial<OpenAPIConfig>, HttpRequest: HttpRequestConstructor = FetchHttpRequest) {
        this.request = new HttpRequest({
            BASE: config?.BASE ?? 'http://localhost',
            VERSION: config?.VERSION ?? '0',
            WITH_CREDENTIALS: config?.WITH_CREDENTIALS ?? false,
            CREDENTIALS: config?.CREDENTIALS ?? 'include',
            TOKEN: config?.TOKEN,
            USERNAME: config?.USERNAME,
            PASSWORD: config?.PASSWORD,
            HEADERS: config?.HEADERS,
            ENCODE_PATH: config?.ENCODE_PATH,
        });
        this.restAiController = new RestAiControllerService(this.request);
        this.restBazaarController = new RestBazaarControllerService(this.request);
        this.restCircleController = new RestCircleControllerService(this.request);
        this.restCurrentUserInfoController = new RestCurrentUserInfoControllerService(this.request);
        this.restFailureReportController = new RestFailureReportControllerService(this.request);
        this.restFineTuningDataController = new RestFineTuningDataControllerService(this.request);
        this.restGlobalSettingsController = new RestGlobalSettingsControllerService(this.request);
        this.restHealthCheckController = new RestHealthCheckControllerService(this.request);
        this.restLinkController = new RestLinkControllerService(this.request);
        this.restNotebookController = new RestNotebookControllerService(this.request);
        this.restNoteController = new RestNoteControllerService(this.request);
        this.restQuizQuestionController = new RestQuizQuestionControllerService(this.request);
        this.restReviewPointController = new RestReviewPointControllerService(this.request);
        this.restReviewsController = new RestReviewsControllerService(this.request);
        this.restSubscriptionController = new RestSubscriptionControllerService(this.request);
        this.restTextContentController = new RestTextContentControllerService(this.request);
        this.restUserController = new RestUserControllerService(this.request);
        this.restWikidataController = new RestWikidataControllerService(this.request);
        this.testabilityRestController = new TestabilityRestControllerService(this.request);
    }
}

