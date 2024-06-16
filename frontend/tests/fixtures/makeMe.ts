import NoteRealmBuilder from "./NoteRealmBuilder";
import ReviewPointBuilder from "./ReviewPointBuilder";
import LinkBuilder from "./LinkBuilder";
import DueReviewPointsBuilder from "./DueReviewPointsBuilder";
import NotebookBuilder from "./NotebookBuilder";
import CircleNoteBuilder from "./CircleNoteBuilder";
import BazaarNotebooksBuilder from "./BazaarNotebooksBuilder";
import NoteBuilder from "./NoteBuilder";
import UserBuilder from "./UserBuilder";
import WikidataEntityBuilder from "./WikidataEntityBuilder";
import WikidataSearchEntityBuilder from "./WikidataSearchEntityBuilder";
import QuizQuestionInNotebookBuilder from "./QuizQuestionInNotebookBuilder";
import QuizQuestionBuilder from "./QuizQuestionBuilder";
import SuggestedQuestionForFineTuningBuilder from "./SuggestedQuestionForFineTuningBuilder";
import ApiErrorBuilder from "./ApiErrorBuilder";

class MakeMe {
  static aUser() {
    return new UserBuilder();
  }

  static get aNote(): NoteBuilder {
    return new NoteBuilder();
  }

  static get aNoteRealm(): NoteRealmBuilder {
    return new NoteRealmBuilder();
  }

  static get aReviewPoint(): ReviewPointBuilder {
    return new ReviewPointBuilder();
  }

  static get aLink(): LinkBuilder {
    return new LinkBuilder();
  }

  static get aDueReviewPointsList(): DueReviewPointsBuilder {
    return new DueReviewPointsBuilder();
  }

  static get aQuizQuestion(): QuizQuestionBuilder {
    return new QuizQuestionBuilder();
  }

  static get aQuizQuestionInNotebook(): QuizQuestionInNotebookBuilder {
    return new QuizQuestionInNotebookBuilder();
  }

  static get aCircleNote(): CircleNoteBuilder {
    return new CircleNoteBuilder();
  }

  static get aNotebook(): NotebookBuilder {
    return new NotebookBuilder();
  }

  static get bazaarNotebooks(): BazaarNotebooksBuilder {
    return new BazaarNotebooksBuilder();
  }

  static get aWikidataEntity(): WikidataEntityBuilder {
    return new WikidataEntityBuilder();
  }

  static get aWikidataSearchEntity(): WikidataSearchEntityBuilder {
    return new WikidataSearchEntityBuilder();
  }

  static get aSuggestedQuestionForFineTuning(): SuggestedQuestionForFineTuningBuilder {
    return new SuggestedQuestionForFineTuningBuilder();
  }

  static get anApiError(): ApiErrorBuilder {
    return new ApiErrorBuilder().error404();
  }
}

export default MakeMe;
