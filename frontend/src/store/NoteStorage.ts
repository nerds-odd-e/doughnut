import CurrentPosition, {
  CurrentPositionImplementation,
} from "./CurrentPosition";

export default interface NoteStorage extends CurrentPosition {
  updatedNoteRealm?: Generated.NoteRealm;
  uglytemporarySolution?: string;
  storageUpdatedAt?: Date;
  focusOnNotebooks(): void;
  selectPosition(
    note?: Generated.Note,
    notePosition?: Generated.NotePositionViewedByUser,
    circle?: Generated.Circle
  ): void;
  refreshNoteRealm(
    data: Generated.NoteRealm | Generated.NoteRealmWithPosition,
    routerAction: "push" | "replace" | undefined
  ): Generated.NoteRealm;
}

export class StorageImplementation
  extends CurrentPositionImplementation
  implements NoteStorage
{
  updatedNoteRealm?: Generated.NoteRealm;

  storageUpdatedAt?: Date;

  uglytemporarySolution?: string;

  focusOnNotebooks(): void {
    this.selectPosition();
    this.updatedNoteRealm = undefined;
    this.storageUpdatedAt = new Date();
  }

  refreshNoteRealm(
    data: Generated.NoteRealm | Generated.NoteRealmWithPosition,
    routerAction: "push" | "replace" | undefined
  ): Generated.NoteRealm {
    let noteRealm: Generated.NoteRealm;
    if (data && "noteRealm" in data) {
      noteRealm = data.noteRealm;
      this.selectPosition(noteRealm.note, data.notePosition);
    } else {
      noteRealm = data;
    }
    this.uglytemporarySolution = routerAction;
    this.updatedNoteRealm = noteRealm;
    this.storageUpdatedAt = new Date();
    return noteRealm;
  }
}
