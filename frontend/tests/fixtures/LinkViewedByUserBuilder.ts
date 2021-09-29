import Builder from "./Builder"
import NoteBuilder from "./NoteBuilder" 
import BreadcrumbBuilder from "./BreadcrumbBuilder"

class LinkViewedByUserBuilder extends Builder {
  data: any;

  constructor(parentBuilder?: Builder) {
    super(parentBuilder);
  }

  do(): any {
    return {
      id: 8,
      linkTypeLabel: "a link",
      sourceNoteViewedByUser:  {
        noteItself: new NoteBuilder().do(),
        noteBreadcrumbViewedByUser: new BreadcrumbBuilder().do()
      },
      targetNoteViewedByUser: {
        noteItself: new NoteBuilder().do(),
        noteBreadcrumbViewedByUser: new BreadcrumbBuilder().do()
      }
    }
  }
}

export default LinkViewedByUserBuilder
