import { screen } from "@testing-library/vue";
import Sidebar from "@/components/notes/Sidebar.vue";
import { NoteRealm } from "@/generated/backend";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

function isBefore(node1: Node, node2: Node) {
  return !!(
    // eslint-disable-next-line no-bitwise
    (node1.compareDocumentPosition(node2) & Node.DOCUMENT_POSITION_FOLLOWING)
  );
}

describe("Sidebar", () => {
  const topNoteRealm = makeMe.aNoteRealm.topicConstructor("top").please();
  const firstGeneration = makeMe.aNoteRealm
    .topicConstructor("first gen")
    .under(topNoteRealm)
    .please();
  const firstGenerationSibling = makeMe.aNoteRealm
    .topicConstructor("first gen sibling")
    .under(topNoteRealm)
    .please();
  const secondGeneration = makeMe.aNoteRealm
    .topicConstructor("2nd gen")
    .under(firstGeneration)
    .please();

  const render = (n: NoteRealm) => {
    helper
      .component(Sidebar)
      .withStorageProps({
        noteRealm: n,
      })
      .render();
  };

  beforeEach(() => {
    helper.managedApi.restNoteController.show1 = vitest.fn();
  });

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
    expect(helper.managedApi.restNoteController.show1).toBeCalledWith(
      topNoteRealm.id,
    );
  });

  it("should have siblings", async () => {
    helper.managedApi.restNoteController.show1 = vitest
      .fn()
      .mockResolvedValue(topNoteRealm);
    render(firstGeneration);
    await screen.findByText(firstGenerationSibling.note.topic);
  });

  it("should have child note of active first gen", async () => {
    helper.managedApi.restNoteController.show1 = vitest
      .fn()
      .mockResolvedValue(topNoteRealm);
    render(firstGeneration);
    const secondGen = await screen.findByText(secondGeneration.note.topic);
    const sibling = await screen.findByText(firstGenerationSibling.note.topic);
    expect(isBefore(secondGen, sibling)).toBe(true);
  });
});
