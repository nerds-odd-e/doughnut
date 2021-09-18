import LinksBuilder from "./LInksBuilder";
import NoteBuilder from "./NoteBuilder";

class MakeMe {
  get links(): LinksBuilder {
    return new LinksBuilder();
  }
  get aNote(): NoteBuilder {
    return new NoteBuilder();
  }
}

const makeMe = new MakeMe();

export default makeMe;
