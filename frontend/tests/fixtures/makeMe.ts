import LinksBuilder from "./LInksBuilder";
import NoteBuilder from "./NoteBuilder";
import BreadcrumbBuilder from "./BreadcrumbBuilder";

class MakeMe {
  get links(): LinksBuilder {
    return new LinksBuilder();
  }
  get aNote(): NoteBuilder {
    return new NoteBuilder();
  }

  get aBreadcrumb(): BreadcrumbBuilder {
    return new BreadcrumbBuilder();
  }
}

const makeMe = new MakeMe();

export default makeMe;
