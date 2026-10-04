import { TextContentController } from "@generated/donut-backend-api/sdk.gen"
import NoteEditableContent from "@/components/notes/core/NoteEditableContent.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import type Quill from "quill"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"

/** Mounts an editable note (Markdown mode unless overridden) and lets it settle. */
export async function mountNoteEditableContent(
  props: {
    noteId: number
    noteContent?: string
    readonly?: boolean
    asMarkdown?: boolean
    wikiLinks?: string[]
  },
  options: { attachTo?: HTMLElement } = {}
) {
  const wrapper = helper
    .component(NoteEditableContent)
    .withCleanStorage()
    .withRouter()
    .withProps({ readonly: false, asMarkdown: true, wikiLinks: [], ...props })
    .mount(options)
  await flushPromises()
  return wrapper
}

export function textareaEl(wrapper: VueWrapper<ComponentPublicInstance>) {
  return wrapper.find("textarea").element as HTMLTextAreaElement
}

export async function setTextareaValue(
  wrapper: VueWrapper<ComponentPublicInstance>,
  value: string
) {
  const el = textareaEl(wrapper)
  el.value = value
  el.dispatchEvent(new Event("input"))
  await flushPromises()
  return el
}

export async function blurTextarea(
  wrapper: VueWrapper<ComponentPublicInstance>
) {
  await wrapper.find("textarea").trigger("blur")
  await flushPromises()
}

/** Dispatches a real textarea paste of `html` (and optional `text/plain`). */
export async function pasteIntoTextarea(
  textarea: HTMLTextAreaElement,
  html: string,
  plainText?: string
) {
  const clipboardData = new DataTransfer()
  clipboardData.setData("text/html", html)
  if (plainText !== undefined) clipboardData.setData("text/plain", plainText)
  textarea.dispatchEvent(
    new ClipboardEvent("paste", {
      bubbles: true,
      cancelable: true,
      clipboardData,
    })
  )
  await flushPromises()
}

/** Mounts note 1 in Markdown mode, focuses its textarea, selects `selection`
 * (default: the end), and pastes into it. */
export async function mountAndPaste(
  noteContent: string,
  html: string,
  options: { plainText?: string; selection?: [number, number] } = {}
) {
  const wrapper = await mountNoteEditableContent(
    { noteId: 1, noteContent },
    { attachTo: document.body }
  )
  const textarea = textareaEl(wrapper)
  textarea.focus()
  if (options.selection) textarea.setSelectionRange(...options.selection)
  await pasteIntoTextarea(textarea, html, options.plainText)
  return { wrapper, textarea }
}

export function choiceShown(wrapper: VueWrapper<ComponentPublicInstance>) {
  return wrapper.find('[data-testid="paste-choice"]').exists()
}

/** Applies the pending paste choice's "Use original text" action. */
export async function useOriginalText(
  wrapper: VueWrapper<ComponentPublicInstance>
) {
  await wrapper.find('[data-testid="paste-choice-action"]').trigger("click")
  await flushPromises()
}

export function richQuillEditorEl(
  wrapper: VueWrapper<ComponentPublicInstance>
): HTMLElement {
  return wrapper.find(".ql-editor").element as HTMLElement
}

export function richQuillInstance(
  wrapper: VueWrapper<ComponentPublicInstance>
): Quill {
  const quillComponent = wrapper.findComponent({ name: "QuillEditor" })
  // biome-ignore lint/suspicious/noExplicitAny: Quill instance is not part of the public API
  return (quillComponent.vm as any).quill as Quill
}

/** Dispatches a real rich (Quill) paste `ClipboardEvent` over the given selection. */
export async function dispatchRichPaste(
  wrapper: VueWrapper<ComponentPublicInstance>,
  html: string,
  options: { plainText?: string; selection: { index: number; length?: number } }
) {
  const editorEl = richQuillEditorEl(wrapper)
  editorEl.focus()
  const { index, length = 0 } = options.selection
  richQuillInstance(wrapper).setSelection(index, length)

  const clipboardData = new DataTransfer()
  clipboardData.setData("text/html", html)
  if (options.plainText !== undefined) {
    clipboardData.setData("text/plain", options.plainText)
  }
  editorEl.dispatchEvent(
    new ClipboardEvent("paste", {
      bubbles: true,
      cancelable: true,
      clipboardData,
    })
  )
  await flushPromises()
}

export function setupUpdateNoteContentMock() {
  return mockSdkService(
    TextContentController,
    "updateNoteContent",
    makeMe.aNoteRealm.please()
  )
}
