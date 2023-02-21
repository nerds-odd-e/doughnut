import { Ref, ref } from "vue";
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

  cache: Map<Doughnut.ID, Ref<Generated.NoteRealm | undefined>> = new Map();

  focusOnNotebooks(): void {
    this.selectPosition();
    this.updatedNoteRealm = undefined;
    this.storageUpdatedAt = new Date();
  }

  refreshNoteRealm(noteRealm: Generated.NoteRealm): Generated.NoteRealm {
    this.updatedNoteRealm = noteRealm;
    this.refOfNoteRealm(noteRealm?.id).value = noteRealm;
    this.storageUpdatedAt = new Date();
    return noteRealm;
  }

  refOfNoteRealm(noteId: Doughnut.ID): Ref<Generated.NoteRealm | undefined> {
    if (!this.cache.has(noteId)) {
      this.cache.set(noteId, ref(undefined));
    }
    return this.cache.get(noteId) as Ref<Generated.NoteRealm | undefined>;
  }
}
