import { flushPromises } from "@vue/test-utils"
import { expandPropertyPanel, propertyRowSelector } from "./propertiesTestDom"
import {
  atViewport,
  DRAFT,
  expectApart,
  expectWholeTextVisibleInRow,
  interactiveControls,
  KEY_INPUT,
  PRESET_LIST,
  rect,
  VALUE_INPUT,
} from "./propertyLayoutTestSupport"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const markdown = `---
url: https://example.com/a/b
topic:
  - alpha
  - alpha
---

Body`

function expectTouchSized(root: Element) {
  const controls = interactiveControls(root)
  expect(controls.length).toBeGreaterThan(1)
  for (const control of controls) {
    expect(rect(control).height).toBeGreaterThanOrEqual(44)
  }
}

describe("RichMarkdownEditor property touch targets", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  beforeEach(async () => {
    await atViewport(375)
  })

  const mount = () => h.mountEditor(markdown, { attachToBody: true })

  it("runs on a device reporting a coarse pointer", () => {
    expect(matchMedia("(pointer: coarse)").matches).toBe(true)
  })

  it.each(["url", "topic"])(
    "gives the %s row's property actions touch-sized targets",
    async (key) => {
      const wrapper = await mount()
      await expandPropertyPanel(wrapper, propertyRowSelector(key))

      expectTouchSized(wrapper.get(propertyRowSelector(key)).element)
    }
  )

  it("aligns short stored properties in one touch-sized row and wraps a long URL within its row", async () => {
    const wrapper = await h.mountEditor(
      `---
type: Note
wikidata_id: Q133900
url: https://en.m.wikipedia.org/wiki/Diffraction
parent: "[[Physics]]"
---

Body`,
      { attachToBody: true }
    )

    for (const key of ["type", "parent"]) {
      const row = wrapper.get(propertyRowSelector(key)).element
      const keyField = row.querySelector(KEY_INPUT)!
      const value = row.querySelector(VALUE_INPUT)!
      expect(rect(keyField).top).toBeCloseTo(rect(value).top, 0)
      expect(rect(row).height).toBeLessThanOrEqual(48)
    }
    const wikidataRow = wrapper.get(propertyRowSelector("wikidata_id")).element
    expect(rect(wikidataRow).height).toBeLessThanOrEqual(48)
    const urlRow = wrapper.get(propertyRowSelector("url")).element
    expectWholeTextVisibleInRow(
      urlRow,
      urlRow.querySelector(VALUE_INPUT)!,
      wrapper.element
    )
    const list = wrapper.get('[data-testid="rich-note-property-list"]').element
    expect(rect(list).height).toBeLessThan(280)
    for (const button of list.querySelectorAll(".daisy-btn-square")) {
      expect(rect(button).width).toBeGreaterThanOrEqual(44)
    }
  })

  it("gives a draft's key presets, value and Add/Cancel touch-sized targets apart from each other", async () => {
    const wrapper = await mount()
    await h.openAddProperty()
    ;(wrapper.get(`${DRAFT} ${KEY_INPUT}`).element as HTMLElement).focus()
    await flushPromises()

    const draft = wrapper.get(DRAFT).element
    expectTouchSized(draft)
    expectApart(
      rect(wrapper.get(PRESET_LIST).element),
      rect(wrapper.get(`${DRAFT} ${VALUE_INPUT}`).element)
    )
  })
})
