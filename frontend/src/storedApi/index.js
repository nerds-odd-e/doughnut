import { restGet } from "../restful/restful";

const storedApiGetNoteWithDescendents = async (store, noteId, loadingRef) => {
    const res = await restGet(
        `/api/notes/${noteId}/overview`,
        loadingRef);
    store.commit("loadNotes", res.notes);
    return res;
}


export { storedApiGetNoteWithDescendents }