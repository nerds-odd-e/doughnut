import { flushPromises } from "@vue/test-utils"
import { primeSoftKeyboard } from "@/utils/focusTarget"
import { mockCoarsePointer } from "@tests/helpers/mockCoarsePointer"
import {
  mountSoftKeyboardPrimer,
  softKeyboardPrimerElement,
  waitUntilFocused,
} from "@tests/helpers/softKeyboardPrimerTestSupport"
import {
  clickWikidataSearchResult,
  clickWikidataTitleAction,
  expectReplaceTitleAndAddAliasControls,
  mockWikidataSearchResult,
  mountWikidataDialogReady,
  useWikidataAssociationDialogTestLifecycle,
  wikidataInput,
  wikidataSaveButton,
} from "@tests/notes/wikidataAssociationDialogTestSupport"
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest"

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRoute: () => ({
      path: "/",
    }),
  }
})

const { mountDialog, getSdkSpies, trackWrapper } =
  useWikidataAssociationDialogTestLifecycle()

describe("WikidataAssociationDialog title actions and save", () => {
  let matchMediaSpy: ReturnType<typeof mockCoarsePointer> | undefined

  afterEach(() => {
    matchMediaSpy?.mockRestore()
    matchMediaSpy = undefined
  })

  it.each([
    { searchKey: "dog", label: "dog" },
    { searchKey: "DOG", label: "Dog" },
  ])(
    "emits selected with no titleAction when titles match ($searchKey / $label)",
    async ({ searchKey, label }) => {
      const searchResult = mockWikidataSearchResult(
        getSdkSpies().searchWikidataSpy,
        label,
        "Q11399"
      )
      const dialog = mountDialog(searchKey)
      await flushPromises()
      await clickWikidataSearchResult("Q11399")
      const emitted = dialog.emitted("selected")?.[0]
      expect(emitted?.[0]).toEqual(searchResult)
      expect(emitted?.[1]).toBeUndefined()
      expect(wikidataInput().value).toBe("Q11399")
    }
  )

  it("emits replace then append from the title actions", async () => {
    const { wrapper: dialog, searchResult } = await mountWikidataDialogReady({
      searchWikidataSpy: getSdkSpies().searchWikidataSpy,
      searchKey: "dog",
      searchLabel: "Canine",
      wikidataId: "Q11399",
    })
    trackWrapper(dialog)
    await clickWikidataSearchResult("Q11399")
    expectReplaceTitleAndAddAliasControls("Canine")

    await clickWikidataTitleAction("Replace")
    expect(dialog.emitted("selected")?.[0]).toEqual([searchResult, "replace"])

    await clickWikidataTitleAction("Append")
    expect(dialog.emitted("selected")?.[1]).toEqual([searchResult, "append"])
  })

  it("defers selected until Save when showSaveButton is true", async () => {
    mockWikidataSearchResult(getSdkSpies().searchWikidataSpy, "dog", "Q11399")
    const dialog = mountDialog("dog", { showSaveButton: true })
    await flushPromises()
    await clickWikidataSearchResult("Q11399")
    expect(dialog.emitted("selected")).toBeFalsy()
    expect(dialog.emitted("update:modelValue")?.[0]).toEqual(["Q11399"])
    expect(wikidataSaveButton()).toBeTruthy()

    wikidataSaveButton().click()
    await flushPromises()
    expect(dialog.emitted("save")?.[0]).toEqual(["Q11399"])
  })

  it("enables saving a cleared value only while it differs from the saved value", async () => {
    const dialog = mountDialog("dog", {
      showSaveButton: true,
      canSaveEmptyToClear: true,
      savedValue: "Q123",
      modelValue: "Q123",
    })
    const saveButton = wikidataSaveButton()
    expect(saveButton.disabled).toBe(true)

    const input = wikidataInput()
    input.value = ""
    input.dispatchEvent(new Event("input", { bubbles: true }))
    await flushPromises()
    expect(saveButton.disabled).toBe(false)

    saveButton.click()
    expect(dialog.emitted("save")?.[0]).toEqual([""])

    await dialog.setProps({ modelValue: "", savedValue: "" })
    expect(saveButton.disabled).toBe(true)
  })

  describe("soft keyboard primer", () => {
    beforeEach(() => {
      vi.useFakeTimers({ toFake: ["requestAnimationFrame"] })
      mountSoftKeyboardPrimer()
    })

    afterEach(() => {
      vi.useRealTimers()
    })

    it("transfers focus to wikidata ID input after mount when showSaveButton", async () => {
      matchMediaSpy = mockCoarsePointer(true)
      const primer = softKeyboardPrimerElement()
      expect(primer).toBeTruthy()
      primeSoftKeyboard()
      expect(document.activeElement).toBe(primer)
      mountDialog("test", { showSaveButton: true })
      await flushPromises()
      await waitUntilFocused("#wikidataID-wikidataID")
    })
  })
})
