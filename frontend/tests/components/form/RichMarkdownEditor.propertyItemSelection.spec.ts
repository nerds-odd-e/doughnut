import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { userEvent } from "vitest/browser"
import {
  notePropertyItemLocation,
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"
import { mountEditorOnNoteShow } from "./propertyValueDialogTestDom"
import { expandPropertyPanel, propertyRowSelector } from "./propertiesTestDom"

const rows = '[data-test="assimilation-mode-row-UNDERSTANDING"]'
const content = (items: string[]) =>
  `---\nactivity: ${JSON.stringify(items)}\n---\nBody.`

describe("selecting authored property items", () => {
  const h = createRichMarkdownEditorTestHarness()
  beforeEach(() =>
    mockSdkService(NoteController, "getNoteInfo", { memoryTrackers: [] })
  )
  afterEach(() => {
    h.cleanup()
    vi.restoreAllMocks()
  })

  it("opens the first authored item and replaces to a selected duplicate from row space, preserving query", async () => {
    const wrapper = await mountEditorOnNoteShow(
      h,
      content(["run", "run", ""]),
      { route: { ...noteShowLocation(42), query: { conversation: "true" } } }
    )
    const router = wrapper.vm.$router
    const replace = vi.spyOn(router, "replace")
    const push = vi.spyOn(router, "push")
    await expandPropertyPanel(wrapper, propertyRowSelector("activity"))
    expect(router.currentRoute.value).toMatchObject(
      notePropertyItemLocation(42, "activity", { value: "run", occurrence: 1 })
    )
    const second = wrapper.findAll(rows)[1]!.element as HTMLElement
    const box = second.getBoundingClientRect()
    const target = document.elementFromPoint(
      box.right - 3,
      box.top + 3
    ) as HTMLElement
    expect(second.contains(target)).toBe(true)
    target.click()
    await flushPromises()
    expect(router.currentRoute.value).toMatchObject({
      ...notePropertyItemLocation(42, "activity", {
        value: "run",
        occurrence: 2,
      }),
      query: { conversation: "true" },
    })
    expect(
      wrapper.findAll('[data-property-value-focused="true"]')
    ).toHaveLength(1)
    expect(replace).toHaveBeenCalledTimes(2)
    expect(push).not.toHaveBeenCalled()
  })

  it.each([false, true])(
    "selects an authored blank item with the keyboard (readonly %s)",
    async (readonly) => {
      const wrapper = await mountEditorOnNoteShow(h, content(["run", ""]), {
        readonly,
        route: notePropertyLocation(42, "activity"),
      })
      const button = wrapper.get('button[aria-label="Select activity item 2"]')
        .element as HTMLButtonElement
      button.focus()
      expect(document.activeElement).toBe(button)
      await userEvent.keyboard("{Enter}")
      await flushPromises()
      expect(wrapper.vm.$router.currentRoute.value).toMatchObject(
        notePropertyItemLocation(42, "activity", { value: "", occurrence: 1 })
      )
      expect(
        wrapper.findAll(rows)[1]!.attributes("data-property-value-focused")
      ).toBe("true")
    }
  )

  it("opens an empty authored list at its property location", async () => {
    const wrapper = await mountEditorOnNoteShow(h, content([]))
    await expandPropertyPanel(wrapper, propertyRowSelector("activity"))
    expect(wrapper.vm.$router.currentRoute.value).toMatchObject(
      notePropertyLocation(42, "activity")
    )
    expect(
      wrapper.get('[data-testid="rich-note-property-panel"]').isVisible()
    ).toBe(true)
  })
})
