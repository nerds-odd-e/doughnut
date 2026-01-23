import WikidataSearchByLabel from "@/components/notes/WikidataSearchByLabel.vue"
import helper from "@tests/helpers"
import { describe, it, expect, vi, afterEach } from "vitest"
import { type VueWrapper } from "@vue/test-utils"

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRoute: () => ({
      path: "/",
    }),
  }
})

describe("WikidataSearchByLabel", () => {
  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: VueWrapper<any>

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  const mountComponent = (modelValue?: string) => {
    wrapper = helper
      .component(WikidataSearchByLabel)
      .withProps({
        searchKey: "test",
        modelValue,
      })
      .mount({ attachTo: document.body })
    return wrapper
  }

  const getButton = (wrapper: ReturnType<typeof mountComponent>) =>
    wrapper.find("button")

  it.each`
    modelValue   | expectedClasses                               | notExpectedClasses
    ${undefined} | ${["daisy-btn-outline", "daisy-btn-neutral"]} | ${["daisy-btn-primary"]}
    ${""}        | ${["daisy-btn-outline", "daisy-btn-neutral"]} | ${["daisy-btn-primary"]}
    ${"   "}     | ${["daisy-btn-outline", "daisy-btn-neutral"]} | ${["daisy-btn-primary"]}
    ${"Q123"}    | ${["daisy-btn-primary"]}                      | ${["daisy-btn-outline", "daisy-btn-neutral"]}
  `(
    "shows correct button style when modelValue is $modelValue",
    ({ modelValue, expectedClasses, notExpectedClasses }) => {
      const wrapper = mountComponent(modelValue)
      const button = getButton(wrapper)

      expectedClasses.forEach((cls: string) => {
        expect(button.classes()).toContain(cls)
      })
      notExpectedClasses.forEach((cls: string) => {
        expect(button.classes()).not.toContain(cls)
      })
    }
  )

  it("updates button style when modelValue changes", async () => {
    const wrapper = mountComponent()
    const button = getButton(wrapper)

    expect(button.classes()).toContain("daisy-btn-outline")
    expect(button.classes()).toContain("daisy-btn-neutral")

    await wrapper.setProps({ modelValue: "Q456" })

    expect(button.classes()).toContain("daisy-btn-primary")
    expect(button.classes()).not.toContain("daisy-btn-outline")
    expect(button.classes()).not.toContain("daisy-btn-neutral")
  })
})
