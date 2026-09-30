import { noteShowLocation } from "@/routes/noteShowLocation"
import { wikiLinkFromAuthoredToken } from "@/utils/wikiLinkMarkup"
import { propertyRowSelector } from "./propertiesTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const propertiesMarkdown = `---
related: "[[Other]]"
topic: training
wikidata_id: Q42
url: https://example.com/a
relation: an-example-of
---

Body`

describe("RichMarkdownEditor read-only properties", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  async function mountReadOnly() {
    return h.mountEditor(propertiesMarkdown, {
      readonly: true,
      lastSavedMarkdown: propertiesMarkdown,
      wikiLinks: [wikiLinkFromAuthoredToken("Other", 42)],
    })
  }

  it("shows a whole wiki-link value as a link to that note and other text as plain text", async () => {
    const wrapper = await mountReadOnly()

    const link = wrapper.get(`${propertyRowSelector("related")} dd a`)
    expect(link.text()).toBe("Other")
    expect(JSON.parse(link.attributes("to") ?? "{}")).toEqual(
      noteShowLocation(42)
    )
    const topic = wrapper.get(`${propertyRowSelector("topic")} dd`)
    expect(topic.text()).toBe("training")
    expect(topic.find("a").exists()).toBe(false)
  })

  it("keeps Wikidata ID, URL, relation label and plain text values", async () => {
    const wrapper = await mountReadOnly()
    const dd = (key: string) => wrapper.get(`${propertyRowSelector(key)} dd`)
    const externalLink = '[data-testid="rich-note-property-external-link"]'

    expect(dd("wikidata_id").text()).toContain("Q42")
    expect(dd("wikidata_id").find(externalLink).exists()).toBe(true)
    expect(dd("url").text()).toContain("https://example.com/a")
    expect(dd("url").find(externalLink).exists()).toBe(true)
    expect(dd("relation").text()).toBe("an example of")
    expect(dd("topic").text()).toBe("training")
  })

  it("offers no editing or memory controls", async () => {
    const wrapper = await mountReadOnly()
    const list = wrapper.get("dl")

    expect(
      list
        .findAll("button")
        .filter(
          (b) =>
            b.attributes("data-testid") !== "rich-note-property-external-link"
        )
    ).toHaveLength(0)
    expect(list.findAll("input")).toHaveLength(0)
    expect(
      list.find('[data-testid="rich-note-property-row-value-input"]').exists()
    ).toBe(false)
    expect(list.find('[data-testid="rich-note-property-panel"]').exists()).toBe(
      false
    )
    expect(list.text()).not.toMatch(/assimilate|skip/i)
    expect(
      document.querySelector(
        '[data-testid="rich-note-property-key-preset-option"]'
      )
    ).toBeNull()
  })
})
