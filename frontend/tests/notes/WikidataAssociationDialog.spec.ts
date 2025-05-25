import WikidataAssociationDialog from "@/components/notes/WikidataAssociationDialog.vue"
import type { Note } from "@/generated/backend"
import { VueWrapper, flushPromises } from "@vue/test-utils"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

describe("Save wikidata id", () => {
  const wikidataId = "Q123"
  async function putWikidataIdAndSubmit(note: Note) {
    const wrapper = helper
      .component(WikidataAssociationDialog)
      .withStorageProps({
        note,
      })
      .mount()
    wrapper.find("#wikidataID-wikidataID").setValue(wikidataId)
    await wrapper.find('input[value="Save"]').trigger("submit")
    await flushPromises()
    return wrapper
  }

  async function cancelOperation(wrapper: VueWrapper) {
    await wrapper.find('input[type="cancel"]').trigger("click")
    await flushPromises()
  }

  async function confirmDifference(wrapper: VueWrapper) {
    await wrapper.find('input[value="Confirm"]').trigger("submit")
    await flushPromises()
  }

  // eslint-disable-next-line @typescript-eslint/no-empty-function, @typescript-eslint/no-unused-vars
  function doNothing(_: VueWrapper) {
    // noop
  }

  it.each`
    noteTitle   | wikidataTitle | userAction           | shouldSave
    ${"dog"}    | ${"dog"}      | ${doNothing}         | ${true}
    ${"Dog"}    | ${"dog"}      | ${doNothing}         | ${true}
    ${"Canine"} | ${"dog"}      | ${doNothing}         | ${false}
    ${"Canine"} | ${"dog"}      | ${cancelOperation}   | ${false}
    ${"Canine"} | ${"dog"}      | ${confirmDifference} | ${true}
    ${"Canine"} | ${""}         | ${doNothing}         | ${true}
  `(
    "associate $noteTitle with $wikidataTitle and choose to $userAction",
    async ({ noteTitle, wikidataTitle, userAction, shouldSave }) => {
      const note = makeMe.aNote.topicConstructor(noteTitle).please()
      const wikidata = makeMe.aWikidataEntity
        .wikidataTitle(wikidataTitle)
        .please()

      helper.managedApi.restWikidataController.fetchWikidataEntityDataById = vi
        .fn()
        .mockResolvedValue(wikidata)
      helper.managedApi.restNoteController.updateWikidataId = vi
        .fn()
        .mockResolvedValue({})

      const wrapper = await putWikidataIdAndSubmit(note)
      await userAction(wrapper)
      expect(
        helper.managedApi.restWikidataController.fetchWikidataEntityDataById
      ).toBeCalledWith(wikidataId)
      expect(
        helper.managedApi.restNoteController.updateWikidataId
      ).toBeCalledTimes(shouldSave ? 1 : 0)
    }
  )
})
