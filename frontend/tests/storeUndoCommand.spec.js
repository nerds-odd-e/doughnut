/**
 * @jest-environment jsdom
 */
import storeUndoCommand from "../src/storeUndoCommand";
import store from "../src/store/index.js";
import makeMe from "./fixtures/makeMe";

beforeEach(() => {
    fetch.resetMocks();
});

describe("storedApi", () => {
    const note = makeMe.aNote.title("Dummy Title").please()

    describe("delete note", () => {
        beforeEach(async () => {
            store.commit("loadNotes", [note]);
            await storeUndoCommand.initUndoHistory(store, [note])
        });


        test("should init store state noteUndoHistories ", async () => {
            expect(store.state.noteUndoHistories[note.id]).toEqual([note.textContent]);
        });

        test("should push textContent into store state noteUndoHistories ", async () => {
            await storeUndoCommand.addUndoHistory(store,
                {
                    id: note.id,
                    textContent: {
                        description: "Desc",
                        title: 'updatedTitle',
                        updatedAt: "2021-08-24T08:46:44.000+00:00"
                    }
                });

            expect(store.state.noteUndoHistories[note.id].length).toEqual(2);
        });
    });
});

