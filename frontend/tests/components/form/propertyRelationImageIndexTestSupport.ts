import { flushPromises, type VueWrapper } from "@vue/test-utils"
import type { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

type Harness = ReturnType<typeof createRichMarkdownEditorTestHarness>

const RELATION_NOTE = (relation: string) =>
  `---\nrelation: ${relation}\ntype: relationship\n---\n\nBody`

export function editorRoot(h: Harness): HTMLElement {
  return h.getWrapper().element as HTMLElement
}

export async function mountRelationNote(h: Harness, relation: string) {
  await h.mountEditor(RELATION_NOTE(relation))
  await flushPromises()
}

export async function emitQuillBodyHtml(wrapper: VueWrapper, html: string) {
  const quill = wrapper.findComponent({ name: "QuillEditor" })
  quill.vm.$emit("update:modelValue", html)
  await flushPromises()
}

export function propertyRowKeyValues(root: ParentNode): string[] {
  return Array.from(
    root.querySelectorAll('[data-testid="rich-note-property-row-key-input"]')
  ).map((el) => (el as HTMLInputElement).value)
}
