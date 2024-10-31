import NoteToolbar from "@/components/notes/core/NoteToolbar.vue"
import SvgCog from "@/components/svgs/SvgCog.vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

it("displays menu items when dropdown is open", async () => {
  const noteRealm = makeMe.aNoteRealm.topicConstructor("Dummy Title").please()

  const wrapper = helper
    .component(NoteToolbar)
    .withStorageProps({
      note: noteRealm.note,
      audioTools: false,
    })
    .mount()

  // Find the dropdown toggle button
  const dropdownToggleButton = wrapper.findComponent(SvgCog)

  // Simulate a click event on the button to open the dropdown menu
  await dropdownToggleButton.trigger("click")

  // Check if menu items exist
  expect(wrapper.find(".dropdown-menu").exists()).toBe(true)
  expect(wrapper.find(".dropdown-menu").text()).toContain(
    "Questions for the note"
  )
})
