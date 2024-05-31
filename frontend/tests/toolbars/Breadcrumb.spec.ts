import { screen } from "@testing-library/vue";
import Breadcrumb from "@/components/toolbars/Breadcrumb.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("breadcrumb with circles", () => {
  it("render the breadcrumber", async () => {
    const note = makeMe.aNote.please();
    const wrapper = helper
      .component(Breadcrumb)
      .withProps({ noteTopic: note.noteTopic })
      .mount();
    expect(wrapper.find(".breadcrumb-item").text()).toEqual("My Notes");
  });

  it("view note belongs to other people in bazaar", async () => {
    const note = makeMe.aNote.please();
    helper
      .component(Breadcrumb)
      .withProps({ fromBazaar: true, noteTopic: note.noteTopic })
      .render();
    await screen.findByText("Bazaar");
  });

  it("show ancestors in correct order", async () => {
    const parentNote = makeMe.aNote.topicConstructor("parent").please();
    const child = makeMe.aNote
      .topicConstructor("child")
      .underNote(parentNote)
      .please();
    const grandChild = makeMe.aNote.underNote(child).please();
    helper
      .component(Breadcrumb)
      .withProps({ noteTopic: grandChild.noteTopic })
      .render();
    const items = screen.getAllByText(/parent|child/);
    expect(items[0]).toHaveTextContent("parent");
    expect(items[1]).toHaveTextContent("child");
  });
});
