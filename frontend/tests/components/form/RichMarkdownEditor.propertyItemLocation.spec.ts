import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService } from "@tests/helpers"
import { afterEach, beforeEach, describe, expect, it } from "vitest"
import {
  notePropertyItemLocation,
  notePropertyLocation,
} from "@/routes/noteShowLocation"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"
import { mountEditorOnNoteShow } from "./propertyValueDialogTestDom"
import { attemptRenamePropertyKey } from "./propertiesTestDom"

const content = (items: string[], key = "activity") =>
  `---\n${key}: ${JSON.stringify(items)}\n---\nReadable body.`
const selected =
  '[data-test="assimilation-mode-row-UNDERSTANDING"][data-property-value-focused="true"]'
const unresolved = '[data-testid="rich-note-property-item-not-found"]'
const item = { value: "run", occurrence: 2 }

describe("property item location", () => {
  const h = createRichMarkdownEditorTestHarness()
  beforeEach(() =>
    mockSdkService(NoteController, "getNoteInfo", { memoryTrackers: [] })
  )
  afterEach(() => h.cleanup())

  it.each([false, true])(
    "opens the exact duplicate in a fresh editor (readonly %s)",
    async (readonly) => {
      const wrapper = await mountEditorOnNoteShow(
        h,
        content(["run", "run", "walk"]),
        { readonly, route: notePropertyItemLocation(42, "activity", item) }
      )
      const rows = wrapper.findAll(
        '[data-test="assimilation-mode-row-UNDERSTANDING"]'
      )
      expect(rows[1]!.attributes("data-property-value-focused")).toBe("true")
      expect(wrapper.findAll(selected)).toHaveLength(1)
      const style = getComputedStyle(rows[1]!.element)
      expect(style.borderInlineStartWidth).toBe("3px")
      expect(style.backgroundColor).not.toBe("rgba(0, 0, 0, 0)")
      expect(wrapper.text()).toContain("Readable body.")
    }
  )

  it("exposes the existing scalar value frame as the single focused value", async () => {
    const wrapper = await mountEditorOnNoteShow(
      h,
      "---\ntopic: movement\n---\nBody.",
      { route: notePropertyLocation(42, "topic") }
    )
    const frames = wrapper.findAll('[data-property-value-focused="true"]')
    expect(frames).toHaveLength(1)
    expect(frames[0]!.attributes("data-testid")).toBe(
      "rich-note-property-value-frame"
    )
    expect(
      wrapper.get('[data-testid="rich-note-property-panel"]').isVisible()
    ).toBe(true)
  })

  it("retains a distinct value destination after reordering and unrelated removal", async () => {
    const location = notePropertyItemLocation(42, "activity", {
      value: "run",
      occurrence: 1,
    })
    const wrapper = await mountEditorOnNoteShow(
      h,
      content(["walk", "run", "jog"]),
      { route: location }
    )
    await wrapper.setProps({ modelValue: content(["jog", "run"]) })
    expect(wrapper.get(selected).attributes("data-property-value")).toBe("run")
    expect(wrapper.vm.$router.currentRoute.value).toMatchObject(location)
  })

  it.each([
    ["walk", "jog"],
    ["run", "walk"],
  ])(
    "shows stale-item feedback for missing text/occurrence: %j",
    async (...items) => {
      const wrapper = await mountEditorOnNoteShow(h, content(items), {
        route: notePropertyItemLocation(42, "activity", item),
      })
      expect(wrapper.get(unresolved).text()).toBe(
        'Item "run" (occurrence 2) not found in property "activity"'
      )
      expect(
        wrapper.get('[data-testid="rich-note-property-panel"]').isVisible()
      ).toBe(true)
      expect(wrapper.text()).toContain("Readable body.")
      expect(wrapper.findAll(selected)).toHaveLength(0)
    }
  )

  it("keeps a read-only stale property readable", async () => {
    const wrapper = await mountEditorOnNoteShow(h, content(["walk"]), {
      readonly: true,
      route: notePropertyItemLocation(42, "activity", item),
    })
    expect(wrapper.get(unresolved).text()).toContain('Item "run"')
    expect(wrapper.text()).toContain("walk")
  })

  it.each([false, true])(
    "keeps a scalar replacement unresolved without selecting its value (readonly %s)",
    async (readonly) => {
      const location = notePropertyItemLocation(42, "activity", item)
      const wrapper = await mountEditorOnNoteShow(h, content(["run", "run"]), {
        readonly,
        route: location,
      })
      await wrapper.setProps({
        modelValue: "---\nactivity: movement\n---\nReadable body.",
      })

      expect(wrapper.get(unresolved).text()).toContain('Item "run"')
      expect(
        wrapper.get('[data-testid="rich-note-property-value-frame"]').text()
      ).toContain("movement")
      expect(
        wrapper.findAll('[data-property-value-focused="true"]')
      ).toHaveLength(0)
      expect(wrapper.vm.$router.currentRoute.value).toMatchObject(location)
    }
  )

  it("opens a property-only list destination without an item selection", async () => {
    const wrapper = await mountEditorOnNoteShow(h, content(["run", "walk"]), {
      route: notePropertyLocation(42, "activity"),
    })
    expect(
      wrapper.get('[data-testid="rich-note-property-panel"]').isVisible()
    ).toBe(true)
    expect(wrapper.findAll(selected)).toHaveLength(0)
  })

  it("follows an authored-key rename preserving the item and conversation query", async () => {
    const wrapper = await mountEditorOnNoteShow(h, content(["run", "run"]), {
      route: {
        ...notePropertyItemLocation(42, "activity", item),
        query: { conversation: "true" },
      },
    })
    await attemptRenamePropertyKey(wrapper, 0, "example of")
    expect(wrapper.vm.$router.currentRoute.value).toMatchObject({
      ...notePropertyItemLocation(42, "example of", item),
      query: { conversation: "true" },
    })
    expect(wrapper.get(selected).attributes("data-property-value")).toBe("run")
  })
})
