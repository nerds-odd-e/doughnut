import { flushPromises } from "@vue/test-utils"
import { expandPropertyPanel, propertyRowSelector } from "./propertiesTestDom"
import {
  atViewport,
  DRAFT,
  expectApart,
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
