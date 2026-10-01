import { CUSTOM_RELATION_RADIO_SENTINEL } from "@/models/relationTypeOptions"
import { flushPromises } from "@vue/test-utils"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property value controls", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("adds an image property from a typed URL", async () => {
    const wrapper = await h.mountEditor("# Hi")

    await h.openAddProperty()
    await wrapper
      .find('[data-testid="rich-note-property-key"]')
      .setValue("image")
    await flushPromises()

    const valInput = wrapper.find('[data-testid="rich-note-property-value"]')
    await valInput.setValue("https://example.com/a.png")
    await valInput.trigger("blur")

    expect(h.lastEmittedMarkdown()).toContain(
      "image: https://example.com/a.png"
    )
  })

  it("updates an existing image property from typed text", async () => {
    const wrapper = await h.mountEditor(
      "---\nimage: https://example.com/old.png\n---\n\n# Hi"
    )

    const valInput = wrapper.find(
      '[data-testid="rich-note-property-row-value-input"]'
    )
    await valInput.setValue("https://example.com/new.png")
    await valInput.trigger("blur")

    expect(h.lastEmittedMarkdown()).toContain(
      "image: https://example.com/new.png"
    )
  })

  describe("relation property in rich mode", () => {
    const mountRelationNote = (relation: string) =>
      h.mountEditor(
        `---\nrelation: ${relation}\ntype: Relationship\n---\n\nBody`
      )
    const relationTypeButton = () =>
      h.getWrapper().get('[aria-label="Relation Type"]')

    it.each([
      {
        relation: "similar-to",
        expectedLabel: "similar to",
        notExpected: "similar-to",
      },
      {
        relation: "my-custom-relation",
        expectedLabel: "my-custom-relation",
        notExpected: "related to",
      },
    ])(
      "relation button shows $expectedLabel for $relation",
      async ({ relation, expectedLabel, notExpected }) => {
        await mountRelationNote(relation)
        const text = relationTypeButton().text()
        expect(text).toContain(expectedLabel)
        expect(text).not.toContain(notExpected)
      }
    )

    it("opens custom relation dialog prefilled and commits updated frontmatter", async () => {
      await mountRelationNote("xyz-unknown-kebab")
      await relationTypeButton().trigger("click")
      await flushPromises()

      expect(document.querySelector("dialog")?.textContent).toContain("Custom…")
      const input = document.querySelector(
        "dialog input[type='text'].daisy-input"
      ) as HTMLInputElement
      expect(input.value).toBe("xyz-unknown-kebab")
      expect(
        document
          .querySelector(
            `label[for="rich-note-relation-property-${CUSTOM_RELATION_RADIO_SENTINEL}"]`
          )
          ?.classList.contains("bg-primary")
      ).toBe(true)

      input.value = "novel connector phrase"
      input.dispatchEvent(new Event("input", { bubbles: true }))
      input.dispatchEvent(
        new KeyboardEvent("keydown", { key: "Enter", bubbles: true })
      )
      await flushPromises()
      expect(h.lastEmittedMarkdown()).toContain(
        "relation: novel-connector-phrase"
      )
    })
  })
})
