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
  expectReplaceTitleAndAddAliasControls,
  mockWikidataSearchResult,
  mountWikidataDialogReady,
  selectWikidataSearchResultWithTitleAction,
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

  it("emits selected with no titleAction when titles match", async () => {
    const searchResult = mockWikidataSearchResult(
      getSdkSpies().searchWikidataSpy,
      "dog",
      "Q11399"
    )
    const dialog = mountDialog("dog")
    await flushPromises()
    await clickWikidataSearchResult("Q11399")
    const emitted = dialog.emitted("selected")?.[0]
    expect(emitted?.[0]).toEqual(searchResult)
    expect(emitted?.[1]).toBeUndefined()
    expect(wikidataInput().value).toBe("Q11399")
  })

  it("emits selected with no titleAction when titles match case-insensitively", async () => {
    const searchResult = mockWikidataSearchResult(
      getSdkSpies().searchWikidataSpy,
      "Dog",
      "Q11399"
    )
    const dialog = mountDialog("DOG")
    await flushPromises()
    await clickWikidataSearchResult("Q11399")
    const emitted = dialog.emitted("selected")?.[0]
    expect(emitted?.[0]).toEqual(searchResult)
    expect(emitted?.[1]).toBeUndefined()
  })

  it("shows replace title and add alias controls when suggested title differs", async () => {
    mockWikidataSearchResult(
      getSdkSpies().searchWikidataSpy,
      "Canine",
      "Q11399"
    )
    mountDialog("dog")
    await flushPromises()
    await clickWikidataSearchResult("Q11399")
    expectReplaceTitleAndAddAliasControls("Canine")
  })

  it.each([false, true])(
    "emits selected with replace action when showSaveButton is %s",
    async (showSaveButton) => {
      const { wrapper: dialog, searchResult } = await mountWikidataDialogReady({
        searchWikidataSpy: getSdkSpies().searchWikidataSpy,
        searchKey: "dog",
        searchLabel: "Canine",
        wikidataId: "Q11399",
        mountOptions: showSaveButton ? { showSaveButton: true } : undefined,
      })
      trackWrapper(dialog)
      await selectWikidataSearchResultWithTitleAction("Q11399", "Replace")
      const emitted = dialog.emitted("selected")?.[0]
      expect(emitted?.[0]).toEqual(searchResult)
      expect(emitted?.[1]).toBe("replace")
    }
  )

  it.each([false, true])(
    "emits selected with add alias action when showSaveButton is %s",
    async (showSaveButton) => {
      const { wrapper: dialog, searchResult } = await mountWikidataDialogReady({
        searchWikidataSpy: getSdkSpies().searchWikidataSpy,
        searchKey: "dog",
        searchLabel: "Canine",
        wikidataId: "Q11399",
        mountOptions: showSaveButton ? { showSaveButton: true } : undefined,
      })
      trackWrapper(dialog)
      await selectWikidataSearchResultWithTitleAction("Q11399", "Append")
      const emitted = dialog.emitted("selected")?.[0]
      expect(emitted?.[0]).toEqual(searchResult)
      expect(emitted?.[1]).toBe("append")
    }
  )

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

  it("enables Save and emits empty string when clearing with canSaveEmptyToClear", async () => {
    const dialog = mountDialog("dog", {
      showSaveButton: true,
      canSaveEmptyToClear: true,
      savedValue: "Q123",
      modelValue: "Q123",
    })
    await flushPromises()
    const input = wikidataInput()
    input.value = ""
    input.dispatchEvent(new Event("input", { bubbles: true }))
    await flushPromises()
    const saveButton = wikidataSaveButton()
    expect(saveButton.disabled).toBe(false)
    saveButton.click()
    await flushPromises()
    expect(dialog.emitted("save")?.[0]).toEqual([""])
  })

  it("disables Save when current value equals savedValue", async () => {
    mountDialog("dog", {
      showSaveButton: true,
      modelValue: "Q123",
      savedValue: "Q123",
    })
    await flushPromises()
    expect(wikidataSaveButton().disabled).toBe(true)
  })

  it("disables Save when both current and saved are empty", async () => {
    mountDialog("dog", {
      showSaveButton: true,
      canSaveEmptyToClear: true,
      modelValue: "",
      savedValue: "",
    })
    await flushPromises()
    expect(wikidataSaveButton().disabled).toBe(true)
  })

  describe("soft keyboard primer", () => {
    beforeEach(() => {
      mountSoftKeyboardPrimer()
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
