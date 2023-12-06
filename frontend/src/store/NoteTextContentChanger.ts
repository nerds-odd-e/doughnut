import { DebouncedFunc } from "lodash";

export default class NoteTextContentChanger {
  changer: DebouncedFunc<
    (
      noteId: number,
      newValue: Generated.TextContent,
      oldValue: Generated.TextContent,
      errorHander: (errs: unknown) => void,
    ) => void
  >;

  constructor(
    changer: DebouncedFunc<
      (
        noteId: number,
        newValue: Generated.TextContent,
        oldValue: Generated.TextContent,
        errorHander: (errs: unknown) => void,
      ) => void
    >,
  ) {
    this.changer = changer;
  }

  change(
    noteId: number,
    newValue: Generated.TextContent,
    oldValue: Generated.TextContent,
    errorHander: (errs: unknown) => void,
  ): void {
    this.changer(noteId, newValue, oldValue, errorHander);
  }

  flush(): void {
    this.changer.flush();
  }

  cancel(): void {
    this.changer.cancel();
  }
}
