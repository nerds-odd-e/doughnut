import LinksBuilder from "./LInksBuilder";
import NoteBuilder from "./NoteBuilder";
import BreadcrumbBuilder from "./BreadcrumbBuilder";
import ReviewPointBuilder from "./ReviewPointBuilder"
import LinkViewedByUserBuilder from "./LinkViewedByUserBuilder"

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

  get aReviewPoint(): ReviewPointBuilder {
    return new ReviewPointBuilder();
  }

  get aLinkViewedByUser(): LinkViewedByUserBuilder {
    return new LinkViewedByUserBuilder();
  }

}

const makeMe = new MakeMe();

export default makeMe;
