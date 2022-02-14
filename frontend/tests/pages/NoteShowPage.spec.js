/**
 * @jest-environment jsdom
 */
 import { screen } from "@testing-library/vue";
 import NoteShowPage from "@/pages/NoteShowPage.vue";
 import NoteWithLinks from "@/components/notes/NoteWithLinks.vue";
 import store from "../../src/store/index.js";
 import { renderWithStoreAndMockRoute, 
  mountWithStoreAndMockRoute } from "../helpers";
 import flushPromises from "flush-promises";
 import _ from "lodash";
 import makeMe from "../fixtures/makeMe";
 import { viewType } from "../../src/models/viewTypes";

 jest.useFakeTimers();
 
 beforeEach(() => {
   fetch.resetMocks();
 });


 describe("all in note show page", () => {
 
 describe("note show", () => {
   test(" should fetch API to be called TWICE when viewType is not included ", async () => {
     const note = makeMe.aNote.please()
     const stubResponse = {
       notePosition: makeMe.aNotePosition.inCircle('a circle').please(),
       notes: [ note ]
     };
     fetch.mockResponse(JSON.stringify(stubResponse));
     renderWithStoreAndMockRoute(store, NoteShowPage, {
       propsData: { noteId: note.id },
     });
     await flushPromises();
     jest.advanceTimersByTime(5000);
     expect(fetch).toHaveBeenCalledTimes(3);
     expect(fetch).toHaveBeenCalledWith(`/api/notes/${note.id}`, {});
     await screen.findByText("a circle");
   });

   test(" should fetch API to be called when viewType is mindmap ", async () => {
    const note = makeMe.aNote.please()
    const stubResponse = {
      notePosition: makeMe.aNotePosition.inCircle('a circle').please(),
      notes: [ note ]
    };
    const viewTypeValue = "mindmap";
    fetch.mockResponse(JSON.stringify(stubResponse));
    renderWithStoreAndMockRoute(store, NoteShowPage, {
      propsData: { noteId: note.id, viewType: viewTypeValue },
    });
    await flushPromises();
    //jest.advanceTimersByTime(5000);
    expect(viewType(viewTypeValue).fetchAll).toBe(true);
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(`/api/notes/${note.id}/overview`, {});
    await screen.findByText("a circle");
  
  });



 });

 

 describe("polling data", () => {
  it("should not call fetch API when inputing text ", async () => {
    const note = makeMe.aNote.title("Dummy Title").please();
    store.commit("loadNotes", [note]);

    const { wrapper } = mountWithStoreAndMockRoute(store, NoteWithLinks, {
      props: {
        note: note
      },
    });

    await wrapper.find('[role="title"]').trigger("click");
    await wrapper.find('[role="title"] input').trigger("input");
    

    expect(wrapper.emitted()["on-editing"]).toHaveLength(1);
    expect(wrapper.emitted()["on-editing"][0][0]).toBe("onEditing");

    await flushPromises();
    jest.advanceTimersByTime(5000);
    expect(fetch).not.toHaveBeenCalledWith(
      `/api/notes/${note.id}`,{}
    );
  });

});

});
 