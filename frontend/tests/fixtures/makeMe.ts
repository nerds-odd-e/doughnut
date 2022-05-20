import LinksBuilder from "./LinksBuilder";
import NoteRealmBuilder from "./NoteRealmBuilder";
import NotePositionBuilder from "./NotePositionBuilder";
import ReviewPointBuilder from "./ReviewPointBuilder";
import LinkBuilder from "./LinkBuilder";
import RepetitionBuilder from "./RepetitionBuilder";
import NotebookBuilder from "./NotebookBuilder";
import CircleNoteBuilder from "./CircleNoteBuilder";
import BazaarNoteBuilder from "./NotebooksBuilder";
import NoteBuilder from "./NoteBuilder";
import UserBuilder from "./UserBuilder";

class MakeMe {
  static aUser() {
    return new UserBuilder();
  }

  static get links(): LinksBuilder {
    return new LinksBuilder();
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
