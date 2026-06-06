import { richModeKeyDropdownPresetKeysForPropertyRows } from "@/utils/noteContentFrontmatter"
import { flushPromises } from "@vue/test-utils"
import { mockCoarsePointer } from "@tests/helpers/mockCoarsePointer"
import { nextTick } from "vue"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property key presets", () => {
  const h = createRichMarkdownEditorTestHarness()
  let matchMediaSpy: ReturnType<typeof mockCoarsePointer> | undefined

  afterEach(() => {
    matchMediaSpy?.mockRestore()
    h.cleanup()
  })

  it("inserting a property emits composed frontmatter and preserves body", async () => {
    await h.mountEditor("# Hello Body")
    await h.openAddProperty()

    const keyInput = h
      .getWrapper()
      .find('[data-testid="rich-note-property-key"]')
    const valInput = h
      .getWrapper()
      .find('[data-testid="rich-note-property-value"]')
    await keyInput.setValue("status")
    await h.setWikiPropertyValueField(valInput, "draft")
    await valInput.trigger("blur")

    const last = h.lastEmittedMarkdown()
    expect(last).toContain("---")
    expect(last).toContain("status: draft")
    expect(last).toContain("Hello Body")
  })

  it.each([
    {
      case: "insert row",
      markdown: "# Hello Body",
      keyInputTestId: "rich-note-property-key",
      existingRows: [] as { key: string; value: string }[],
    },
    {
      case: "existing row",
      markdown: `---
status: ok
---

# Body`,
      keyInputTestId: "rich-note-property-row-key-input",
      existingRows: [{ key: "status", value: "ok" }],
    },
  ] as const)("shows preset keys when $case key input is focused", async ({
    markdown,
    keyInputTestId,
    existingRows,
  }) => {
    await h.mountEditor(markdown, { attachToBody: true })
    if (keyInputTestId === "rich-note-property-key") {
      await h.openAddProperty()
    }
    ;(
      h.getWrapper().find(`[data-testid="${keyInputTestId}"]`)
        .element as HTMLInputElement
    ).focus()
    await nextTick()
    await flushPromises()
    await h.assertPresetOptionsVisible(
      richModeKeyDropdownPresetKeysForPropertyRows(false, existingRows)
    )
  })

  it.each([
    {
      case: "insert",
      markdown: "# Hello Body",
      keyInputTestId: "rich-note-property-key",
      presetKey: "wikidata_id",
    },
    {
      case: "existing row",
      markdown: `---
status: ok
---

# Body`,
      keyInputTestId: "rich-note-property-row-key-input",
      presetKey: "url",
    },
  ] as const)("sets property key when a preset is chosen ($case)", async ({
    markdown,
    keyInputTestId,
    presetKey,
  }) => {
    await h.mountEditor(markdown, { attachToBody: true })
    if (keyInputTestId === "rich-note-property-key") {
      await h.openAddProperty()
    }
    ;(
      h.getWrapper().find(`[data-testid="${keyInputTestId}"]`)
        .element as HTMLInputElement
    ).focus()
    await nextTick()
    await h.selectPresetKey(presetKey)
    expect(
      (
        h.getWrapper().find(`[data-testid="${keyInputTestId}"]`)
          .element as HTMLInputElement
      ).value
    ).toBe(presetKey)
  })

  it("property key preset dropdown resolves occupied presets to suffixed keys", async () => {
    const markdown = `---
image: /x.png
---

# Body`
    await h.mountEditor(markdown, { attachToBody: true })
    await h.openAddProperty()
    ;(
      h.getWrapper().find('[data-testid="rich-note-property-key"]')
        .element as HTMLInputElement
    ).focus()
    await nextTick()
    await flushPromises()
    await h.assertPresetOptionsVisible(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [
        { key: "image", value: "/x.png" },
      ])
    )
  })

  it("selecting a resolved preset inserts the suffixed key when base is taken", async () => {
    const markdown = `---
url: https://example.com
---

# Body`
    await h.mountEditor(markdown, { attachToBody: true })
    await h.openAddProperty()
    ;(
      h.getWrapper().find('[data-testid="rich-note-property-key"]')
        .element as HTMLInputElement
    ).focus()
    await nextTick()
    await flushPromises()
    await h.selectPresetKey("url 2")
    expect(
      (
        h.getWrapper().find('[data-testid="rich-note-property-key"]')
          .element as HTMLInputElement
      ).value
    ).toBe("url 2")
  })
})
