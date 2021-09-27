import { restGet } from "../restful/restful";

const storedApiGetNoteWithDescendents = async (store, noteId) => {
    const res = await restGet(
        `/api/notes/${noteId}/overview`);
    store.commit("loadNotes", res.notes);
    return res;
}

const x = 1

export { storedApiGetNoteWithDescendents, x }