import LinksBuilder from "./LInksBuilder";
import NoteSphereBuilder from "./NoteSphereBuilder";
import NotePositionBuilder from "./NotePositionBuilder";
import ReviewPointBuilder from "./ReviewPointBuilder"
import LinkViewedByUserBuilder from "./LinkViewedByUserBuilder"
import RepetitionBuilder from "./RepetitionBuilder"
import NotebookBuilder from "./NotebookBuilder";
import CircleNoteBuilder from "./CircleNoteBuilder";
import BazaarNoteBuilder from "./BazaarNotebooksBuilder";

class MakeMe {
  static get links(): LinksBuilder {
    return new LinksBuilder();
  }

  static get aNoteSphere(): NoteSphereBuilder {
    return new NoteSphereBuilder();
  }

  static get aNotePosition(): NotePositionBuilder {
    return new NotePositionBuilder();
  }

  static get aReviewPoint(): ReviewPointBuilder {
    return new ReviewPointBuilder();
  }

  static get aLinkViewedByUser(): LinkViewedByUserBuilder {
    return new LinkViewedByUserBuilder();
  }

  static get aRepetition(): RepetitionBuilder {
    return new RepetitionBuilder();
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
}

export default MakeMe;
