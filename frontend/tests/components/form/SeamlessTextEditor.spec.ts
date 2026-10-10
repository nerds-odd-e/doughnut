import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { nextTick } from "vue"
import {
  editorEl,
  focusEditor,
  insertAtSelection,
  mountSeamlessTextEditor,
  PASTE_NO_UPDATE_CASES,
  PASTE_SUCCESS_CASES,
  pasteClipboard,
  setCaretInEditor,
} from "./seamlessTextEditorTestSupport"

function moveFocusToAButton() {
  const button = document.createElement("button")
  document.body.appendChild(button)
  button.focus()
}

describe("SeamlessTextEditor", () => {
  let wrapper: VueWrapper

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  it("does not emit update when the change is from initial value", async () => {
    wrapper = await mountSeamlessTextEditor("initial value")
    expect(wrapper.emitted()["update:modelValue"]).toBeUndefined()
  })

  it("keeps caret offset when modelValue is synced with same-length text", async () => {
    wrapper = await mountSeamlessTextEditor(
      "x:y",
      {},
      { attachTo: document.body }
    )
    const editor = editorEl(wrapper)
    editor.focus()
    await nextTick()
    const textNode = editor.firstChild as Text
    const selection = window.getSelection()
    const range = document.createRange()
    range.setStart(textNode, 1)
    range.setEnd(textNode, 1)
    selection?.removeAllRanges()
    selection?.addRange(range)

    await wrapper.setProps({ modelValue: "x：y" })
    await nextTick()

    const sel = window.getSelection()
    expect(sel?.rangeCount).toBe(1)
    const r = sel?.getRangeAt(0)
    expect(r?.startOffset).toBe(1)
    expect(r?.startContainer).toBe(editor.firstChild)
  })

  it.each(PASTE_SUCCESS_CASES)(
    "pastes plain text ($case)",
    async ({ initialValue, caretOffset, paste, expected }) => {
      wrapper = await mountSeamlessTextEditor(
        initialValue,
        {},
        { attachTo: document.body }
      )
      const editor = editorEl(wrapper)
      await focusEditor(editor)
      if (caretOffset !== undefined) {
        setCaretInEditor(editor, caretOffset)
      }
      await pasteClipboard(editor, paste)

      expect(wrapper.emitted()["update:modelValue"]?.at(-1)?.[0]).toBe(expected)
      expect(editor.innerText).toBe(expected)
    }
  )

  it("inserts at the selection it remembered when focus left the editor", async () => {
    wrapper = await mountSeamlessTextEditor(
      "Orchard notes",
      {},
      { attachTo: document.body }
    )
    const editor = editorEl(wrapper)
    await focusEditor(editor)
    setCaretInEditor(editor, 0, 7)
    moveFocusToAButton()
    window.getSelection()?.removeAllRanges()

    await insertAtSelection(wrapper, () => "Garden")

    expect(wrapper.emitted()["update:modelValue"]?.at(-1)?.[0]).toBe(
      "Garden notes"
    )
    expect(editor.innerText).toBe("Garden notes")
    const range = window.getSelection()?.getRangeAt(0)
    expect(range?.collapsed).toBe(true)
    expect(range?.startContainer).toBe(editor.firstChild)
    expect(range?.startOffset).toBe(6)
  })

  it("appends inserted text when no selection was ever placed", async () => {
    wrapper = await mountSeamlessTextEditor(
      "Orchard",
      {},
      { attachTo: document.body }
    )

    await insertAtSelection(wrapper, (before, after) => `[${before}|${after}]`)

    expect(wrapper.emitted()["update:modelValue"]?.at(-1)?.[0]).toBe(
      "Orchard[Orchard|]"
    )
  })

  it("appends inserted text after its text was replaced from outside", async () => {
    wrapper = await mountSeamlessTextEditor(
      "Orchard notes",
      {},
      { attachTo: document.body }
    )
    const editor = editorEl(wrapper)
    await focusEditor(editor)
    setCaretInEditor(editor, 7)
    moveFocusToAButton()
    await wrapper.setProps({ modelValue: "Pear tree" })

    await insertAtSelection(wrapper, () => " care")

    expect(editor.innerText).toBe("Pear tree care")
  })

  it("focuses the editor with the caret after the inserted text", async () => {
    wrapper = await mountSeamlessTextEditor(
      "Orchard",
      {},
      { attachTo: document.body }
    )
    const editor = editorEl(wrapper)

    await insertAtSelection(wrapper, () => " notes")

    expect(document.activeElement).toBe(editor)
    const range = window.getSelection()?.getRangeAt(0)
    expect(range?.collapsed).toBe(true)
    expect(range?.startOffset).toBe(13)
  })

  it("submits the nearest form on Enter", async () => {
    const onSubmit = vi.fn((e: Event) => e.preventDefault())
    const form = document.createElement("form")
    form.addEventListener("submit", onSubmit)
    const submit = document.createElement("input")
    submit.type = "submit"
    form.appendChild(submit)

    wrapper = await mountSeamlessTextEditor("x", {}, { attachTo: form })
    document.body.appendChild(form)

    const editor = editorEl(wrapper)
    await focusEditor(editor)
    editor.dispatchEvent(
      new KeyboardEvent("keydown", {
        key: "Enter",
        bubbles: true,
        cancelable: true,
      })
    )
    await flushPromises()
    await nextTick()

    expect(onSubmit).toHaveBeenCalledTimes(1)
    form.remove()
  })

  it.each(PASTE_NO_UPDATE_CASES)(
    "does not handle paste when $case",
    async ({ initialValue, props, paste, expectedInnerText }) => {
      wrapper = await mountSeamlessTextEditor(initialValue, props)
      const editor = editorEl(wrapper)
      await focusEditor(editor)
      await pasteClipboard(editor, paste)

      expect(wrapper.emitted()["update:modelValue"]).toBeUndefined()
      if (expectedInnerText !== undefined) {
        expect(editor.innerText).toBe(expectedInnerText)
      }
    }
  )
})
