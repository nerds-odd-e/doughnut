import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import RichMarkdownEditor from "@/components/form/RichMarkdownEditor.vue"
import NoteToolbar from "@/components/notes/core/NoteToolbar.vue"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import GlobalApiLoadingModal from "@tests/helpers/GlobalApiLoadingModal"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { defineComponent, ref } from "vue"
import { afterEach, expect, it } from "vitest"

let wrapper: VueWrapper

afterEach(() => {
  wrapper?.unmount()
  teardownGlobalClientForTesting()
  document.body.innerHTML = ""
})

it("keeps the rich editor mounted while a property rename is checked, then shows the renamed source", async () => {
  let finishGuard!: () => void
  const guard = new Promise<void>((resolve) => {
    finishGuard = resolve
  })
  const noteInfo = mockSdkService(
    NoteController,
    "getNoteInfo",
    makeMe.aNoteRecallInfo.please()
  )
  noteInfo.mockImplementation(async () => {
    await guard
    return wrapSdkResponse(makeMe.aNoteRecallInfo.please())
  })
  const noteRealm = makeMe.aNoteRealm
    .content("---\ntopic: wiki\n---\n\nBody.")
    .please()
  const Harness = defineComponent({
    components: { RichMarkdownEditor, NoteToolbar, GlobalApiLoadingModal },
    setup: () => ({
      noteRealm,
      markdown: ref(noteRealm.note.content),
      asMarkdown: ref(false),
    }),
    template: `
      <GlobalApiLoadingModal />
      <NoteToolbar :note="noteRealm.note" :notebook-id="noteRealm.notebookRealm.notebook.id"
        :as-markdown="asMarkdown" @edit-as-markdown="asMarkdown = $event" />
      <textarea v-if="asMarkdown" v-model="markdown" />
      <RichMarkdownEditor v-else v-model="markdown" :note-id="noteRealm.note.id" :wiki-links="[]" />
    `,
  })
  wrapper = helper
    .component(Harness)
    .withCleanStorage()
    .withRouter()
    .mount({ attachTo: document.body })
  await flushPromises()
  const key = wrapper.find('[data-testid="rich-note-property-row-key-input"]')
  await key.trigger("focus")
  await key.setValue("domain")
  await key.trigger("blur")
  await flushPromises()
  expect(noteInfo).toHaveBeenCalled()
  const toggleMode = () =>
    document.dispatchEvent(
      new KeyboardEvent("keydown", {
        key: "m",
        code: "KeyM",
        bubbles: true,
        cancelable: true,
      })
    )
  toggleMode()
  await flushPromises()
  expect(wrapper.find("textarea").exists()).toBe(false)
  expect(document.querySelector("dialog:modal")).not.toBeNull()
  finishGuard()
  await flushPromises()
  expect(document.querySelector("dialog:modal")).toBeNull()
  ;(document.activeElement as HTMLElement)?.blur()
  toggleMode()
  await flushPromises()
  expect(wrapper.find("textarea").element).toHaveValue(
    "---\ndomain: wiki\n---\n\nBody."
  )
})
