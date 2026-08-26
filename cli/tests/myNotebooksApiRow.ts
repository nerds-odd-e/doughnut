import makeMe from 'donut-test-fixtures/makeMe'

/** One element of `NotebooksViewedByUser.notebooks` from `myNotebooks`. */
export function myNotebooksApiRow(name: string) {
  return {
    notebook: makeMe.aNotebook
      .withSeedNote(makeMe.aNote.title(name).please())
      .do(),
  }
}
