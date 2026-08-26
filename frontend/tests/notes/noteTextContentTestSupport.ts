import { TextContentController } from "@generated/doughnut-backend-api/sdk.gen"
import NoteTextContent from "@/components/notes/core/NoteTextContent.vue"
import type { Note, WikiTitle } from "@generated/doughnut-backend-api"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkServiceWithImplementation } from "@tests/helpers"
import { type VueWrapper, flushPromises } from "@vue/test-utils"
import { vi } from "vitest"
import type { ComponentPublicInstance } from "vue"

export const mockedUpdateTitleCall = vi.fn()

export function mockUpdateNoteTitle() {
  mockedUpdateTitleCall.mockImplementation(async (options) =>
    makeMe.aNoteRealm.title(options.body.newTitle).please()
  )
  mockSdkServiceWithImplementation(
    TextContentController,
    "updateNoteTitle",
    async (options) => await mockedUpdateTitleCall(options)
  )
}

export function mountNoteTextContent(
  note: Note,
  options?: {
    readonly?: boolean
    wikiTitles?: WikiTitle[]
  }
): VueWrapper<ComponentPublicInstance> {
  return helper
    .component(NoteTextContent)
    .withCleanStorage()
    .withRouter()
    .withProps({
      readonly: options?.readonly ?? false,
      note,
      wikiTitles: options?.wikiTitles ?? [],
    })
    .mount({ attachTo: document.body })
}

export function titleEditorEl(
  wrapper: VueWrapper<ComponentPublicInstance>
): HTMLElement {
  return wrapper.find('[data-test="note-title"]').element as HTMLElement
}

export async function editTitle(
  wrapper: VueWrapper<ComponentPublicInstance>,
  newValue: string
) {
  await flushPromises()
  const titleEl = titleEditorEl(wrapper)
  titleEl.innerText = newValue
  titleEl.dispatchEvent(new Event("input"))
}

export async function editTitleThenBlur(
  wrapper: VueWrapper<ComponentPublicInstance>,
  newValue = "updated"
) {
  await editTitle(wrapper, newValue)
  titleEditorEl(wrapper).dispatchEvent(new Event("blur"))
}
