import { merge } from 'lodash';
import Builder from "./Builder"
import NoteSphereBuilder from "./NoteSphereBuilder"
import NotePositionBuilder from "./NotePositionBuilder"

class LinkViewedByUserBuilder extends Builder {
  data: any = {};

  do(): any {
    return merge(this.data, {
      id: 8,
      linkTypeLabel: "a link",
      sourceNoteWithPosition: {
        note: new NoteSphereBuilder().do(),
        notePosition: new NotePositionBuilder().do()
      },
      targetNoteWithPosition: {
        note: new NoteSphereBuilder().do(),
        notePosition: new NotePositionBuilder().do()
      }
    });
  }
}

export default LinkViewedByUserBuilder
