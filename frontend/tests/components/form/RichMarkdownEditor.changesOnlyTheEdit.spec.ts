import {
  expandPropertyPanelAndClickRemove,
  propertyRowSelector,
  propertyRows,
} from "./propertiesTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor changes only what the user edited", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  const typeAtStart = async (body: string, word: string) => {
    await h.mountEditor(body)
    h.quillInstance().insertText(0, word, "user")
    return h.lastEmittedMarkdown()
  }

  it("keeps common Markdown forms, changing only the edited line", async () => {
    const body = [
      "## Part",
      "",
      "- item",
      "- other",
      "",
      "Some *em* and **strong** text.",
      "",
      "---",
      "",
      "End.",
    ].join("\n")

    expect(await typeAtStart(body, "My ")).toBe(
      body.replace("## Part", "## My Part")
    )
  })

  it("normalizes other forms to the common ones", async () => {
    const body = ["Intro", "", "+ item", "+ other", "", "Some _em_ text."].join(
      "\n"
    )

    expect(await typeAtStart(body, "An ")).toBe(
      ["An Intro", "", "- item", "- other", "", "Some *em* text."].join("\n")
    )
  })

  it("keeps the authored frontmatter and blank line on a body edit", async () => {
    const note =
      "---\nname: demo2\ndescription: Second skill\ntype: note\ntags: [x]\n---\n\n# Demo2\n"

    expect(await typeAtStart(note, "My ")).toBe(
      note.replace("# Demo2\n", "# My Demo2")
    )
  })

  it("keeps comments, quoting and flow lists in unsorted frontmatter byte for byte", async () => {
    const frontmatter = [
      "---",
      "zeta: 'quoted value'",
      "# author comment",
      "alpha: [b, a]",
      "---",
      "",
    ].join("\n")

    expect(await typeAtStart(`${frontmatter}Body`, "The ")).toBe(
      `${frontmatter}The Body`
    )
  })

  it("lists properties in the order the file has them", async () => {
    const wrapper = await h.mountEditor(
      "---\nname: demo2\ndescription: Second skill\ntype: note\ntags: [x]\n---\n\n# Demo2\n"
    )

    expect(
      propertyRows(wrapper.element).map((row) => row.dataset.propertyKey)
    ).toEqual(["name", "description", "type", "tags"])
  })
  describe("property panel edits", () => {
    const note = [
      "---",
      "name: demo",
      "# a comment",
      'description: "Quoted: value"',
      "tags: [x, y]",
      "---",
      "",
      "Body",
    ].join("\n")

    it("changes only the changed property's line", async () => {
      const wrapper = await h.mountEditor(note)
      const valueInput = wrapper.find(
        `${propertyRowSelector("description")} [data-testid="rich-note-property-row-value-input"]`
      )
      await h.setPropertyValueField(valueInput, "New text")
      await valueInput.trigger("blur")

      expect(h.lastEmittedMarkdown()).toBe(
        note.replace('description: "Quoted: value"', "description: New text")
      )
    })

    it("removes only the removed property's line", async () => {
      const wrapper = await h.mountEditor(note)
      await expandPropertyPanelAndClickRemove(
        wrapper,
        propertyRowSelector("tags")
      )

      expect(h.lastEmittedMarkdown()).toBe(note.replace("tags: [x, y]\n", ""))
    })
  })
})
