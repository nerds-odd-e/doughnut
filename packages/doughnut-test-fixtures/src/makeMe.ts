import type { RecallPrompt } from '@generated/doughnut-backend-api'
import ApiErrorBuilder from './ApiErrorBuilder'
import BookFullBuilder from './BookFullBuilder'
import AnsweredQuestionBuilder from './AnsweredQuestionBuilder'
import AssessmentAttemptBuilder from './AssessmentAttemptBuilder'
import AssessmentQuestionInstanceBuilder from './AssessmentQuestionInstanceBuilder'
import MultipleChoicesQuestionBuilder from './BareQuestionBuilder'
import BazaarNotebooksBuilder from './BazaarNotebooksBuilder'
import CircleBuilder from './CircleBuilder'
import CircleNoteBuilder from './CircleNoteBuilder'
import ConversationBuilder from './ConversationBuilder'
import DueMemoryTrackersBuilder from './DueMemoryTrackersBuilder'
import {
  FailureReportBuilder,
  FailureReportForViewBuilder,
} from './FailureReportBuilder'
import RelationshipBuilder from './RelationshipBuilder'
import MemoryTrackerBuilder from './MemoryTrackerBuilder'
import NoteBuilder from './NoteBuilder'
import NoteRecallInfoBuilder from './NoteRecallInfoBuilder'
import NoteRealmBuilder from './NoteRealmBuilder'
import NotebookBuilder from './NotebookBuilder'
import NotebookCatalogBuilder from './NotebookCatalogBuilder'
import NotebookCatalogGroupItemBuilder from './NotebookCatalogGroupItemBuilder'
import NotebookCatalogNotebookItemBuilder from './NotebookCatalogNotebookItemBuilder'
import NotebookCatalogSubscribedNotebookItemBuilder from './NotebookCatalogSubscribedNotebookItemBuilder'
import PredefinedQuestionBuilder from './PredefinedQuestionBuilder'
import RecallPromptBuilder from './RecallPromptBuilder'
import SuggestedQuestionForFineTuningBuilder from './SuggestedQuestionForFineTuningBuilder'
import NoteSearchResultBuilder from './NoteSearchResultBuilder'
import UserBuilder from './UserBuilder'
import UserForListingBuilder from './UserForListingBuilder'
import UserListingPageBuilder from './UserListingPageBuilder'
import WikidataEntityBuilder from './WikidataEntityBuilder'
import WikidataSearchEntityBuilder from './WikidataSearchEntityBuilder'

class MakeMe {
  static get aUser() {
    return new UserBuilder()
  }

  static get aNote(): NoteBuilder {
    return new NoteBuilder()
  }

  static get aNoteRecallInfo(): NoteRecallInfoBuilder {
    return new NoteRecallInfoBuilder()
  }

  static get aNoteRealm(): NoteRealmBuilder {
    return new NoteRealmBuilder()
  }

  static get aMemoryTracker(): MemoryTrackerBuilder {
    return new MemoryTrackerBuilder()
  }

  static get aRelationship(): RelationshipBuilder {
    return new RelationshipBuilder()
  }

  static get aDueMemoryTrackersList(): DueMemoryTrackersBuilder {
    return new DueMemoryTrackersBuilder()
  }

  static get aRecallPrompt(): RecallPromptBuilder {
    return new RecallPromptBuilder()
  }

  static recallPromptFrom(
    base: RecallPrompt,
    overrides: Partial<RecallPrompt>
  ): RecallPrompt {
    return { ...base, ...overrides }
  }

  static get anAssessmentQuestionInstance(): AssessmentQuestionInstanceBuilder {
    return new AssessmentQuestionInstanceBuilder()
  }

  static get aPredefinedQuestion(): PredefinedQuestionBuilder {
    return new PredefinedQuestionBuilder()
  }

  static get aCircleNote(): CircleNoteBuilder {
    return new CircleNoteBuilder()
  }

  static get aCircle(): CircleBuilder {
    return new CircleBuilder()
  }

  static get aNotebook(): NotebookBuilder {
    return new NotebookBuilder()
  }

  static get notebookCatalogNotebook(): NotebookCatalogNotebookItemBuilder {
    return new NotebookCatalogNotebookItemBuilder()
  }

  static get notebookCatalogGroup(): NotebookCatalogGroupItemBuilder {
    return new NotebookCatalogGroupItemBuilder()
  }

  static get notebookCatalogSubscribedNotebook(): NotebookCatalogSubscribedNotebookItemBuilder {
    return new NotebookCatalogSubscribedNotebookItemBuilder()
  }

  static get notebookCatalog(): NotebookCatalogBuilder {
    return new NotebookCatalogBuilder()
  }

  static get aBook(): BookFullBuilder {
    return new BookFullBuilder()
  }

  static get bazaarNotebooks(): BazaarNotebooksBuilder {
    return new BazaarNotebooksBuilder()
  }

  static get aWikidataEntity(): WikidataEntityBuilder {
    return new WikidataEntityBuilder()
  }

  static get anAssessmentAttempt(): AssessmentAttemptBuilder {
    return new AssessmentAttemptBuilder()
  }

  static get aWikidataSearchEntity(): WikidataSearchEntityBuilder {
    return new WikidataSearchEntityBuilder()
  }

  static get aSuggestedQuestionForFineTuning(): SuggestedQuestionForFineTuningBuilder {
    return new SuggestedQuestionForFineTuningBuilder()
  }

  static get aConversation(): ConversationBuilder {
    return new ConversationBuilder()
  }

  static get anApiError(): ApiErrorBuilder {
    return new ApiErrorBuilder().error404()
  }

  static get anAnsweredQuestion(): AnsweredQuestionBuilder {
    return new AnsweredQuestionBuilder()
  }

  static get aMultipleChoicesQuestion(): MultipleChoicesQuestionBuilder {
    return new MultipleChoicesQuestionBuilder()
  }

  static get aNoteSearchResult(): NoteSearchResultBuilder {
    return new NoteSearchResultBuilder()
  }

  static get aUserForListing(): UserForListingBuilder {
    return new UserForListingBuilder()
  }

  static get aUserListingPage(): UserListingPageBuilder {
    return new UserListingPageBuilder()
  }

  static get aFailureReport(): FailureReportBuilder {
    return new FailureReportBuilder()
  }

  static get aFailureReportForView(): FailureReportForViewBuilder {
    return new FailureReportForViewBuilder()
  }
}

export default MakeMe
export type { NotebookCatalogEntry } from './NotebookCatalogBuilder'
