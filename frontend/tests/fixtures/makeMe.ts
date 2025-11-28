import ApiErrorBuilder from "./ApiErrorBuilder"
import AnsweredQuestionBuilder from "./AnsweredQuestionBuilder"
import AssessmentAttemptBuilder from "./AssessmentAttemptBuilder"
import AssessmentQuestionInstanceBuilder from "./AssessmentQuestionInstanceBuilder"
import MultipleChoicesQuestionBuilder from "./BareQuestionBuilder"
import BazaarNotebooksBuilder from "./BazaarNotebooksBuilder"
import CircleBuilder from "./CircleBuilder"
import CircleNoteBuilder from "./CircleNoteBuilder"
import ConversationBuilder from "./ConversationBuilder"
import DueMemoryTrackersBuilder from "./DueMemoryTrackersBuilder"
import LinkBuilder from "./LinkBuilder"
import MemoryTrackerBuilder from "./MemoryTrackerBuilder"
import NoteBuilder from "./NoteBuilder"
import NoteRealmBuilder from "./NoteRealmBuilder"
import NotebookBuilder from "./NotebookBuilder"
import PredefinedQuestionBuilder from "./PredefinedQuestionBuilder"
import RecallPromptBuilder from "./RecallPromptBuilder"
import SuggestedQuestionForFineTuningBuilder from "./SuggestedQuestionForFineTuningBuilder"
import UserBuilder from "./UserBuilder"
import WikidataEntityBuilder from "./WikidataEntityBuilder"
import WikidataSearchEntityBuilder from "./WikidataSearchEntityBuilder"

class MakeMe {
  static get aUser() {
    return new UserBuilder()
  }

  static get aNote(): NoteBuilder {
    return new NoteBuilder()
  }

  static get aNoteRealm(): NoteRealmBuilder {
    return new NoteRealmBuilder()
  }

  static get aMemoryTracker(): MemoryTrackerBuilder {
    return new MemoryTrackerBuilder()
  }

  static get aLink(): LinkBuilder {
    return new LinkBuilder()
  }

  static get aDueMemoryTrackersList(): DueMemoryTrackersBuilder {
    return new DueMemoryTrackersBuilder()
  }

  static get aRecallPrompt(): RecallPromptBuilder {
    return new RecallPromptBuilder()
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
}

export default MakeMe
