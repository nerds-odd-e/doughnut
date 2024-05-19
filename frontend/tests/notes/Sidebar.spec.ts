import { screen } from "@testing-library/vue";
import Sidebar from "@/components/notes/Sidebar.vue";
import { NoteRealm } from "@/generated/backend";
import { flushPromises } from "@vue/test-utils";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("Sidebar", () => {
  const topNoteRealm = makeMe.aNoteRealm.topicConstructor("top").please();
  const firstGeneration = makeMe.aNoteRealm
    .topicConstructor("first gen")
    .under(topNoteRealm)
    .please();

  const render = (n: NoteRealm) => {
    helper.managedApi.restNoteController.show1 = vitest
      .fn()
      .mockResolvedValue(n);
    helper
      .component(Sidebar)
      .withStorageProps({
        noteRealm: n,
      })
      .render();
  };

  it("should not call the api if top note", async () => {
    render(topNoteRealm);
    expect(helper.managedApi.restNoteController.show1).not.toBeCalled();
  });

  it("should render the children notes", async () => {
    render(topNoteRealm);
    await screen.findByText(firstGeneration.note.topic);
  });

  it("should call the api if not top note", async () => {
    render(firstGeneration);
    await flushPromises();
    expect(helper.managedApi.restNoteController.show1).toBeCalledWith(
      topNoteRealm.id,
    );
  });
});
