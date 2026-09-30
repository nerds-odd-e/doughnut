import { WikidataController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import {
  expectReplaceTitleAndAddAliasControls,
  wikidataInput,
  wikidataModal,
  wikidataSaveButton,
} from "@tests/notes/wikidataAssociationDialogTestSupport"
import { mountEditorOnNoteShow } from "./propertyValueDialogTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor wikidata property dialog with a typed ID", () => {
  const h = createRichMarkdownEditorTestHarness()

  async function typeIdAndSave(label: string, id: string) {
    mockSdkService(WikidataController, "searchWikidata", [])
    mockSdkService(
      WikidataController,
      "fetchWikidataEntityDataById",
      makeMe.aWikidataEntity.wikidataTitle(label).please()
    )
    const wrapper = await mountEditorOnNoteShow(
      h,
      `---\nwikidata_id: ""\n---\n\nBody`,
      { noteTitleForWikidataSearch: "Snake" }
    )
    await wrapper
      .find('[data-testid="rich-note-wikidata-property-edit"]')
      .trigger("click")
    await flushPromises()
    const input = wikidataInput()
    input.value = id
    input.dispatchEvent(new Event("input", { bubbles: true }))
    await flushPromises()
    wikidataSaveButton().click()
    await flushPromises()
    return wrapper
  }

  function savedIds(wrapper: VueWrapper) {
    return (wrapper.emitted("update:modelValue") ?? []).map((e) => e[0])
  }

  afterEach(() => {
    h.cleanup()
  })

  it("shows the title choice when the label differs and the search found nothing", async () => {
    const wrapper = await typeIdAndSave("Douglas Adams", "Q42")

    expectReplaceTitleAndAddAliasControls("Douglas Adams")
    expect(wikidataModal()?.textContent).not.toContain("No Wikidata entries")
    expect(savedIds(wrapper)).toHaveLength(0)
  })

  it("second Save applies the ID, closes, and leaves the title alone", async () => {
    await typeIdAndSave("Douglas Adams", "Q42")

    wikidataSaveButton().click()
    await flushPromises()

    expect(h.lastEmittedMarkdown()).toContain("wikidata_id: Q42")
    expect(wikidataModal()).toBeNull()
  })

  it("saves and closes at once when the label equals the title ignoring case", async () => {
    await typeIdAndSave("sNaKe", "Q42")

    expect(h.lastEmittedMarkdown()).toContain("wikidata_id: Q42")
    expect(wikidataModal()).toBeNull()
  })
})
