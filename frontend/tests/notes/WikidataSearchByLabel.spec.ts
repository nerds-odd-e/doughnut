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

  const buttonClasses = () => wrapper.find("button").classes()

  it.each`
    modelValue   | expectedPrimary
    ${undefined} | ${false}
    ${""}        | ${false}
    ${"   "}     | ${false}
    ${"Q123"}    | ${true}
  `(
    "button is primary=$expectedPrimary when modelValue is $modelValue",
    ({ modelValue, expectedPrimary }) => {
      mountComponent(modelValue)
      const classes = buttonClasses()
      if (expectedPrimary) {
        expect(classes).toContain("daisy-btn-primary")
      } else {
        expect(classes).toContain("daisy-btn-outline")
        expect(classes).toContain("daisy-btn-neutral")
      }
    }
  )

  it("updates to primary style when modelValue becomes a Wikidata ID", async () => {
    mountComponent()
    expect(buttonClasses()).toContain("daisy-btn-outline")

    await wrapper.setProps({ modelValue: "Q456" })

    expect(buttonClasses()).toContain("daisy-btn-primary")
  })

  describe("soft keyboard primer", () => {
    beforeEach(() => {
      mountSoftKeyboardPrimer()
    })

    it("focuses primer synchronously when dialog is opened on touch device", () => {
      matchMediaSpy = mockCoarsePointer(true)
      mountComponent()
      const primer = softKeyboardPrimerElement()
      expect(primer).toBeTruthy()

      wrapper.find("button").element.click()

      expect(document.activeElement).toBe(primer)
    })

    it("does not focus primer when pointer is not coarse", async () => {
      matchMediaSpy = mockCoarsePointer(false)
      mountComponent()
      const primer = softKeyboardPrimerElement()

      wrapper.find("button").element.click()
      await flushPromises()
      await wrapper.vm.$nextTick()

      expect(document.activeElement).not.toBe(primer)
    })
  })
})
