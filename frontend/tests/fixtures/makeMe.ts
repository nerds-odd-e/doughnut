import LinksMapBuilder from "./LinksMapBuilder";
import NoteRealmBuilder from "./NoteRealmBuilder";
import NotePositionBuilder from "./NotePositionBuilder";
import ReviewPointBuilder from "./ReviewPointBuilder";
import LinkBuilder from "./LinkBuilder";
import DueReviewPointsBuilder from "./DueReviewPointsBuilder";
import NotebookBuilder from "./NotebookBuilder";
import CircleNoteBuilder from "./CircleNoteBuilder";
import BazaarNoteBuilder from "./NotebooksBuilder";
import NoteBuilder from "./NoteBuilder";
import UserBuilder from "./UserBuilder";
import WikidataEntityBuilder from "./WikidataEntityBuilder";
import WikidataSearchEntityBuilder from "./WikidataSearchEntityBuilder";
import QuizQuestionBuilder from "./QuizQuestionBuilder";
import SuggestedQuestionForFineTuningBuilder from "./SuggestedQuestionForFineTuningBuilder";

class MakeMe {
  static aUser() {
    return new UserBuilder();
  }

  static get linksMap(): LinksMapBuilder {
    return new LinksMapBuilder();
  }

  static get aNote(): NoteBuilder {
    return new NoteBuilder();
  }

  static get aNoteRealm(): NoteRealmBuilder {
    return new NoteRealmBuilder();
  }

  static get aNotePosition(): NotePositionBuilder {
    return new NotePositionBuilder();
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

  static get aCircleNote(): CircleNoteBuilder {
    return new CircleNoteBuilder();
  }

  static get aNotebook(): NotebookBuilder {
    return new NotebookBuilder();
  }

  static get bazaarNotebooks(): BazaarNoteBuilder {
    return new BazaarNoteBuilder();
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
}

export default MakeMe;
