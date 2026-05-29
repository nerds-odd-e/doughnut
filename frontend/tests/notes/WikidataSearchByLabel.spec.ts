import WikidataSearchByLabel from "@/components/notes/WikidataSearchByLabel.vue"
import helper from "@tests/helpers"
import { mockCoarsePointer } from "@tests/helpers/mockCoarsePointer"
import {
  mountSoftKeyboardPrimer,
  softKeyboardPrimerElement,
} from "@tests/helpers/softKeyboardPrimerTestSupport"
import { describe, it, expect, vi, afterEach, beforeEach } from "vitest"
import { flushPromises, type VueWrapper } from "@vue/test-utils"

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
  let matchMediaSpy: ReturnType<typeof mockCoarsePointer> | undefined

  afterEach(() => {
    matchMediaSpy?.mockRestore()
    matchMediaSpy = undefined
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

  describe("soft keyboard primer", () => {
    beforeEach(() => {
      mountSoftKeyboardPrimer()
    })

    it("focuses primer synchronously when dialog is opened on touch device", () => {
      matchMediaSpy = mockCoarsePointer(true)
      const wrapper = mountComponent()
      const primer = softKeyboardPrimerElement()
      expect(primer).toBeTruthy()

      getButton(wrapper).element.click()

      expect(document.activeElement).toBe(primer)
    })

    it("does not focus primer when pointer is not coarse", async () => {
      matchMediaSpy = mockCoarsePointer(false)
      const wrapper = mountComponent()
      const primer = softKeyboardPrimerElement()

      getButton(wrapper).element.click()
      await flushPromises()
      await wrapper.vm.$nextTick()

      expect(document.activeElement).not.toBe(primer)
    })
  })
})
