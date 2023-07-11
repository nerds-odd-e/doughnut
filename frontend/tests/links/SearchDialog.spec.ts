import { screen } from "@testing-library/vue";
import { flushPromises } from "@vue/test-utils";
import LinkNoteDialog from "@/components/links/LinkNoteDialog.vue";
import MakeMe from "../fixtures/makeMe";
import helper from "../helpers";

describe("LinkNoteDialog", () => {
  it("Search at the top level with no note", async () => {
    helper.component(LinkNoteDialog).withStorageProps({ note: null }).render();
    await screen.findByText("Searching");
    expect(
      await screen.findByLabelText("All My Notebooks And Subscriptions"),
    ).toBeDisabled();
  });

  it("Search from a note", async () => {
    const note = MakeMe.aNote.please();
    helper.component(LinkNoteDialog).withStorageProps({ note }).render();
    await screen.findByText(`Link to`);
  });

  it("toggle search settings", async () => {
    const note = MakeMe.aNote.please();
    helper.component(LinkNoteDialog).withStorageProps({ note }).render();
    (await screen.findByLabelText("All My Circles")).click();
    expect(
      await screen.findByLabelText("All My Notebooks And Subscriptions"),
    ).toBeChecked();
    flushPromises();
    (
      await screen.findByLabelText("All My Notebooks And Subscriptions")
    ).click();
    expect(await screen.findByLabelText("All My Circles")).not.toBeChecked();
  });
});
