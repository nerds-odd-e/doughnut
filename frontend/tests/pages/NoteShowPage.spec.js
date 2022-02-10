/**
 * @jest-environment jsdom
 */
 import { screen } from "@testing-library/vue";
 import NoteShowPage from "@/pages/NoteShowPage.vue";
 import store from "../../src/store/index.js";
 import { renderWithStoreAndMockRoute } from "../helpers";
 import flushPromises from "flush-promises";
 import _ from "lodash";
 import makeMe from "../fixtures/makeMe";

 jest.useFakeTimers();
 
 beforeEach(() => {
   fetch.resetMocks();
 });
 
 describe("note show", () => {
   test("fetch API to be called ONCE", async () => {
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
     expect(fetch).toHaveBeenCalledTimes(2);
     expect(fetch).toHaveBeenCalledWith(`/api/notes/${note.id}`, {});
     await screen.findByText("a circle");
   });
 });
 