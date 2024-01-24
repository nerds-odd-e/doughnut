import { DebouncedFunc } from "lodash";

export default class NoteTextContentChanger {
  changer: DebouncedFunc<
    (
      noteId: number,
      newValue: string,
      errorHander: (errs: unknown) => void,
    ) => void
  >;

  constructor(
    changer: DebouncedFunc<
      (
        noteId: number,
        newValue: string,
        errorHander: (errs: unknown) => void,
      ) => void
    >,
  ) {
    this.changer = changer;
  }

  change(
    noteId: number,
    newValue: string,
    errorHander: (errs: unknown) => void,
  ): void {
    this.changer(noteId, newValue, errorHander);
  }

  flush(): void {
    this.changer.flush();
  }

  cancel(): void {
    this.changer.cancel();
  }
}
