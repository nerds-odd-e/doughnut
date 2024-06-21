import ApiErrorBuilder from "./ApiErrorBuilder"
import BazaarNotebooksBuilder from "./BazaarNotebooksBuilder"
import CircleNoteBuilder from "./CircleNoteBuilder"
import DueReviewPointsBuilder from "./DueReviewPointsBuilder"
import LinkBuilder from "./LinkBuilder"
import NoteBuilder from "./NoteBuilder"
import NoteRealmBuilder from "./NoteRealmBuilder"
import NotebookBuilder from "./NotebookBuilder"
import QuizQuestionAndAnswerBuilder from "./QuizQuestionAndAnswerBuilder"
import QuizQuestionBuilder from "./QuizQuestionBuilder"
import QuizQuestionInNotebookBuilder from "./QuizQuestionInNotebookBuilder"
import ReviewPointBuilder from "./ReviewPointBuilder"
import SuggestedQuestionForFineTuningBuilder from "./SuggestedQuestionForFineTuningBuilder"
import UserBuilder from "./UserBuilder"
import WikidataEntityBuilder from "./WikidataEntityBuilder"
import WikidataSearchEntityBuilder from "./WikidataSearchEntityBuilder"

class MakeMe {
  static aUser() {
    return new UserBuilder()
  }

  static get aNote(): NoteBuilder {
    return new NoteBuilder()
  }

  static get aNoteRealm(): NoteRealmBuilder {
    return new NoteRealmBuilder()
  }

  static get aReviewPoint(): ReviewPointBuilder {
    return new ReviewPointBuilder()
  }

  static get aLink(): LinkBuilder {
    return new LinkBuilder()
  }

  static get aDueReviewPointsList(): DueReviewPointsBuilder {
    return new DueReviewPointsBuilder()
  }

  static get aQuizQuestion(): QuizQuestionBuilder {
    return new QuizQuestionBuilder()
  }

  static get aQuizQuestionInNotebook(): QuizQuestionInNotebookBuilder {
    return new QuizQuestionInNotebookBuilder()
  }

  static get aQuizQuestionAndAnswer(): QuizQuestionAndAnswerBuilder {
    return new QuizQuestionAndAnswerBuilder()
  }

  static get aCircleNote(): CircleNoteBuilder {
    return new CircleNoteBuilder()
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

  static get aWikidataSearchEntity(): WikidataSearchEntityBuilder {
    return new WikidataSearchEntityBuilder()
  }

  static get aSuggestedQuestionForFineTuning(): SuggestedQuestionForFineTuningBuilder {
    return new SuggestedQuestionForFineTuningBuilder()
  }

  static get anApiError(): ApiErrorBuilder {
    return new ApiErrorBuilder().error404()
  }
}

export default MakeMe
