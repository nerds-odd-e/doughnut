/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BaseHttpRequest } from './core/BaseHttpRequest';
import type { OpenAPIConfig } from './core/OpenAPI';
import { FetchHttpRequest } from './core/FetchHttpRequest';
import { AssimilationControllerService } from './services/AssimilationControllerService';
import { McpNoteCreationControllerService } from './services/McpNoteCreationControllerService';
import { RestAiAudioControllerService } from './services/RestAiAudioControllerService';
import { RestAiControllerService } from './services/RestAiControllerService';
import { RestAssessmentControllerService } from './services/RestAssessmentControllerService';
import { RestBazaarControllerService } from './services/RestBazaarControllerService';
import { RestCertificateControllerService } from './services/RestCertificateControllerService';
import { RestCircleControllerService } from './services/RestCircleControllerService';
import { RestConversationMessageControllerService } from './services/RestConversationMessageControllerService';
import { RestCurrentUserInfoControllerService } from './services/RestCurrentUserInfoControllerService';
import { RestFailureReportControllerService } from './services/RestFailureReportControllerService';
import { RestFineTuningDataControllerService } from './services/RestFineTuningDataControllerService';
import { RestGlobalSettingsControllerService } from './services/RestGlobalSettingsControllerService';
import { RestHealthCheckControllerService } from './services/RestHealthCheckControllerService';
import { RestLinkControllerService } from './services/RestLinkControllerService';
import { RestMemoryTrackerControllerService } from './services/RestMemoryTrackerControllerService';
import { RestNotebookCertificateApprovalControllerService } from './services/RestNotebookCertificateApprovalControllerService';
import { RestNotebookControllerService } from './services/RestNotebookControllerService';
import { RestNoteControllerService } from './services/RestNoteControllerService';
import { RestNoteCreationControllerService } from './services/RestNoteCreationControllerService';
import { RestPredefinedQuestionControllerService } from './services/RestPredefinedQuestionControllerService';
import { RestRecallPromptControllerService } from './services/RestRecallPromptControllerService';
import { RestRecallsControllerService } from './services/RestRecallsControllerService';
import { RestSearchControllerService } from './services/RestSearchControllerService';
import { RestSubscriptionControllerService } from './services/RestSubscriptionControllerService';
import { RestTextContentControllerService } from './services/RestTextContentControllerService';
import { RestUserControllerService } from './services/RestUserControllerService';
import { RestWikidataControllerService } from './services/RestWikidataControllerService';
import { TestabilityRestControllerService } from './services/TestabilityRestControllerService';
type HttpRequestConstructor = new (config: OpenAPIConfig) => BaseHttpRequest;
export class DoughnutApi {
    public readonly assimilationController: AssimilationControllerService;
    public readonly mcpNoteCreationController: McpNoteCreationControllerService;
    public readonly restAiAudioController: RestAiAudioControllerService;
    public readonly restAiController: RestAiControllerService;
    public readonly restAssessmentController: RestAssessmentControllerService;
    public readonly restBazaarController: RestBazaarControllerService;
    public readonly restCertificateController: RestCertificateControllerService;
    public readonly restCircleController: RestCircleControllerService;
    public readonly restConversationMessageController: RestConversationMessageControllerService;
    public readonly restCurrentUserInfoController: RestCurrentUserInfoControllerService;
    public readonly restFailureReportController: RestFailureReportControllerService;
    public readonly restFineTuningDataController: RestFineTuningDataControllerService;
    public readonly restGlobalSettingsController: RestGlobalSettingsControllerService;
    public readonly restHealthCheckController: RestHealthCheckControllerService;
    public readonly restLinkController: RestLinkControllerService;
    public readonly restMemoryTrackerController: RestMemoryTrackerControllerService;
    public readonly restNotebookCertificateApprovalController: RestNotebookCertificateApprovalControllerService;
    public readonly restNotebookController: RestNotebookControllerService;
    public readonly restNoteController: RestNoteControllerService;
    public readonly restNoteCreationController: RestNoteCreationControllerService;
    public readonly restPredefinedQuestionController: RestPredefinedQuestionControllerService;
    public readonly restRecallPromptController: RestRecallPromptControllerService;
    public readonly restRecallsController: RestRecallsControllerService;
    public readonly restSearchController: RestSearchControllerService;
    public readonly restSubscriptionController: RestSubscriptionControllerService;
    public readonly restTextContentController: RestTextContentControllerService;
    public readonly restUserController: RestUserControllerService;
    public readonly restWikidataController: RestWikidataControllerService;
    public readonly testabilityRestController: TestabilityRestControllerService;
    public readonly request: BaseHttpRequest;
    constructor(config?: Partial<OpenAPIConfig>, HttpRequest: HttpRequestConstructor = FetchHttpRequest) {
        this.request = new HttpRequest({
            BASE: config?.BASE ?? '',
            VERSION: config?.VERSION ?? '0',
            WITH_CREDENTIALS: config?.WITH_CREDENTIALS ?? false,
            CREDENTIALS: config?.CREDENTIALS ?? 'include',
            TOKEN: config?.TOKEN,
            USERNAME: config?.USERNAME,
            PASSWORD: config?.PASSWORD,
            HEADERS: config?.HEADERS,
            ENCODE_PATH: config?.ENCODE_PATH,
        });
        this.assimilationController = new AssimilationControllerService(this.request);
        this.mcpNoteCreationController = new McpNoteCreationControllerService(this.request);
        this.restAiAudioController = new RestAiAudioControllerService(this.request);
        this.restAiController = new RestAiControllerService(this.request);
        this.restAssessmentController = new RestAssessmentControllerService(this.request);
        this.restBazaarController = new RestBazaarControllerService(this.request);
        this.restCertificateController = new RestCertificateControllerService(this.request);
        this.restCircleController = new RestCircleControllerService(this.request);
        this.restConversationMessageController = new RestConversationMessageControllerService(this.request);
        this.restCurrentUserInfoController = new RestCurrentUserInfoControllerService(this.request);
        this.restFailureReportController = new RestFailureReportControllerService(this.request);
        this.restFineTuningDataController = new RestFineTuningDataControllerService(this.request);
        this.restGlobalSettingsController = new RestGlobalSettingsControllerService(this.request);
        this.restHealthCheckController = new RestHealthCheckControllerService(this.request);
        this.restLinkController = new RestLinkControllerService(this.request);
        this.restMemoryTrackerController = new RestMemoryTrackerControllerService(this.request);
        this.restNotebookCertificateApprovalController = new RestNotebookCertificateApprovalControllerService(this.request);
        this.restNotebookController = new RestNotebookControllerService(this.request);
        this.restNoteController = new RestNoteControllerService(this.request);
        this.restNoteCreationController = new RestNoteCreationControllerService(this.request);
        this.restPredefinedQuestionController = new RestPredefinedQuestionControllerService(this.request);
        this.restRecallPromptController = new RestRecallPromptControllerService(this.request);
        this.restRecallsController = new RestRecallsControllerService(this.request);
        this.restSearchController = new RestSearchControllerService(this.request);
        this.restSubscriptionController = new RestSubscriptionControllerService(this.request);
        this.restTextContentController = new RestTextContentControllerService(this.request);
        this.restUserController = new RestUserControllerService(this.request);
        this.restWikidataController = new RestWikidataControllerService(this.request);
        this.testabilityRestController = new TestabilityRestControllerService(this.request);
    }
}

