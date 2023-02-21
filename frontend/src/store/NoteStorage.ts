import CurrentPosition, {
  CurrentPositionImplementation,
} from "./CurrentPosition";

export default interface NoteStorage extends CurrentPosition {
  updatedNoteRealm?: Generated.NoteRealm;
  storageUpdatedAt?: Date;
  focusOnNotebooks(): void;
  selectPosition(
    note?: Generated.Note,
    notePosition?: Generated.NotePositionViewedByUser,
    circle?: Generated.Circle
  ): void;
  refreshNoteRealm(data: Generated.NoteRealm): Generated.NoteRealm;
}

export class StorageImplementation
  extends CurrentPositionImplementation
  implements NoteStorage
{
  updatedNoteRealm?: Generated.NoteRealm;

  storageUpdatedAt?: Date;

  focusOnNotebooks(): void {
    this.selectPosition();
    this.updatedNoteRealm = undefined;
    this.storageUpdatedAt = new Date();
  }

  refreshNoteRealm(noteRealm: Generated.NoteRealm): Generated.NoteRealm {
    this.updatedNoteRealm = noteRealm;
    this.storageUpdatedAt = new Date();
    return noteRealm;
  }
}
