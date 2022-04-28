/**
 * @jest-environment jsdom
 */
import { screen } from "@testing-library/vue";
import NoteShowPage from "@/pages/NoteShowPage.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";
import { viewType } from "../../src/models/viewTypes";

jest.useFakeTimers();

helper.resetWithApiMock(beforeEach, afterEach);

describe("all in note show page", () => {
  describe("note show", () => {
    const note = makeMe.aNoteRealm.please();
    const stubResponse = {
      notePosition: makeMe.aNotePosition
        .for(note.note)
        .inCircle("a circle")
        .please(),
      notes: [note],
    };

    beforeEach(() => {
      // this shouldn't be needed
      // however, the computed notePosition doesn't seem to re-compute after the store value changes.
      helper.store.loadNotesBulk(stubResponse);
    });

    it(" should fetch API to be called TWICE when viewType is not included ", async () => {
      helper.apiMock.expecting(`/api/notes/${note.id}`, stubResponse);
      helper
        .component(NoteShowPage)
        .withProps({ rawNoteId: `${note.id}` })
        .render();
      helper.apiMock.verifyCall(`/api/notes/${note.id}`);
      await screen.findByText(note.note.title);
    });

    it(" should fetch API to be called when viewType is mindmap ", async () => {
      const viewTypeValue = "mindmap";
      helper.apiMock.expecting(`/api/notes/${note.id}/overview`, stubResponse);
      helper
        .component(NoteShowPage)
        .withProps({ rawNoteId: `${note.id}`, viewType: viewTypeValue })
        .render();
      expect(viewType(viewTypeValue)?.fetchAll).toBe(true);
      helper.apiMock.verifyCall(`/api/notes/${note.id}/overview`);
      await screen.findByText(note.note.title);
    });
  });
});
