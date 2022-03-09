import LinksBuilder from "./LInksBuilder";
import NoteBuilder from "./NoteBuilder";
import NotePositionBuilder from "./NotePositionBuilder";
import ReviewPointBuilder from "./ReviewPointBuilder"
import LinkViewedByUserBuilder from "./LinkViewedByUserBuilder"
import RepetitionBuilder from "./RepetitionBuilder"
import NotebookBuilder from "./NotebookBuilder";
import CircleNoteBuilder from "./CircleNoteBuilder";
import BazaarNoteBuilder from "./BazaarNotebooksBuilder";

class MakeMe {
  get links(): LinksBuilder {
    return new LinksBuilder();
  }

  get aNote(): NoteBuilder {
    return new NoteBuilder();
  }

  get aNotePosition(): NotePositionBuilder {
    return new NotePositionBuilder();
  }

  get aReviewPoint(): ReviewPointBuilder {
    return new ReviewPointBuilder();
  }

  get aLinkViewedByUser(): LinkViewedByUserBuilder {
    return new LinkViewedByUserBuilder();
  }

  get aRepetition(): RepetitionBuilder {
    return new RepetitionBuilder();
  }

  get aCircleNote(): CircleNoteBuilder {
    return new CircleNoteBuilder();
  }

  get aNotebook(): NotebookBuilder {
    return new NotebookBuilder();
  }

  get bazaarNotebooks(): BazaarNoteBuilder {
    return new BazaarNoteBuilder();
  }
}

const makeMe = new MakeMe();

export default makeMe;
