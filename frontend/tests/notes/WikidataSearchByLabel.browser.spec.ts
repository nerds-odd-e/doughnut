import WikidataSearchByLabel from "@/components/notes/WikidataSearchByLabel.vue"
import helper from "@tests/helpers"
import { describe, it, expect, vi } from "vitest"

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
  const mountComponent = (modelValue?: string) => {
    return helper
      .component(WikidataSearchByLabel)
      .withProps({
        searchKey: "test",
        modelValue,
      })
      .mount()
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
