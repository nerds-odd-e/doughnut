import NoteTopicComponent from "@/components/notes/core/NoteCoreToolbar.vue";
import ManagedApi from "@/managedApi/ManagedApi";
import createNoteStorage from "../../src/store/createNoteStorage";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";
import SvgCog from "../../src/components/svgs/SvgCog.vue";

it("displays menu items when dropdown is open", async () => {
  const noteStorage = createNoteStorage(
    new ManagedApi({ errors: [], states: [] }),
  );
  const noteRealm = makeMe.aNoteRealm.topicConstructor("Dummy Title").please();
  noteStorage.refreshNoteRealm(noteRealm);

  const wrapper = helper
    .component(NoteTopicComponent)
    .withProps({
      note: noteRealm.note,
      storageAccessor: noteStorage,
    })
    .mount();

  // Find the dropdown toggle button
  const dropdownToggleButton = await wrapper.findComponent(SvgCog);

  // Simulate a click event on the button to open the dropdown menu
  await dropdownToggleButton.trigger("click");

  // Check if menu items exist
  expect(wrapper.find(".dropdown-menu").exists()).toBe(true);
  expect(wrapper.find(".dropdown-menu").text()).toContain("Add Question");
  expect(wrapper.find(".dropdown-menu").text()).toContain("View Questions");
});
