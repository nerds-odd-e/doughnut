import { primeSoftKeyboard } from "@/utils/focusTarget"
import { appendAliasToNoteContent } from "@/utils/wikidataTitleActions"
import { type VueWrapper, flushPromises } from "@vue/test-utils"
import { wrapSdkResponse } from "@tests/helpers"
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
  mountWikidataAssociationDialog,
  mountWikidataDialogReady,
  selectWikidataSearchResultWithTitleAction,
  setupWikidataDialogSdkMocks,
  wikidataInput,
  wikidataModal,
  wikidataSaveButton,
  wikidataSearchResultItem,
  wikidataSearchResults,
  type WikidataDialogSdkSpies,
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

describe("WikidataAssociationDialog", () => {
  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: VueWrapper<any>
  let sdkSpies: WikidataDialogSdkSpies
  let matchMediaSpy: ReturnType<typeof mockCoarsePointer> | undefined

  beforeEach(() => {
    vi.resetAllMocks()
    document.body.innerHTML = ""
    sdkSpies = setupWikidataDialogSdkMocks()
  })

  afterEach(() => {
    matchMediaSpy?.mockRestore()
    matchMediaSpy = undefined
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  const mountDialog = (
    searchKey: string,
    options?: Parameters<typeof mountWikidataAssociationDialog>[1]
  ) => {
    wrapper = mountWikidataAssociationDialog(searchKey, options)
    return wrapper
  }

  describe("basic functionality", () => {
    it("shows the current wikidata ID in the input field", async () => {
      mountDialog("Test Title", { modelValue: "Q123" })
      await flushPromises()
      expect(wikidataInput().value).toBe("Q123")
    })

    it("displays error message in the input field", async () => {
      mountDialog("Test Title", { errorMessage: "Invalid Wikidata ID" })
      await flushPromises()
      const errorMessage = wikidataModal()?.querySelector(".text-error")
      expect(errorMessage?.textContent).toContain("Invalid Wikidata ID")
    })

    it("shows header title", async () => {
      mountDialog("Test Title")
      await flushPromises()
      expect(wikidataModal()?.textContent).toContain("Associate Wikidata")
    })

    it("emits close when close button is clicked", async () => {
      sdkSpies.searchWikidataSpy.mockResolvedValue(wrapSdkResponse([]))
      const wrapper = mountDialog("test")
      await flushPromises()
      const closeButton = wikidataModal()?.querySelector(
        "button.daisy-btn-secondary"
      ) as HTMLButtonElement
      closeButton?.click()
      await flushPromises()
      expect(wrapper.emitted("close")).toBeTruthy()
    })
  })

  describe("search functionality", () => {
    it("shows loading state when searching", async () => {
      sdkSpies.searchWikidataSpy.mockImplementation(
        () =>
          new Promise(() => {
            // biome-ignore lint/suspicious/noExplicitAny: Promise intentionally never resolves for loading state test
          }) as any
      )
      mountDialog("dog")
      await flushPromises()
      expect(wikidataModal()?.textContent).toContain("Searching...")
    })

    it("shows not found message when results are empty and searchKey provided", async () => {
      sdkSpies.searchWikidataSpy.mockResolvedValue(wrapSdkResponse([]))
      mountDialog("nonexistent")
      await flushPromises()
      expect(wikidataModal()?.textContent).toContain(
        "No Wikidata entries found for 'nonexistent'"
      )
    })

    it("shows not found message when searchKey is provided but no results", async () => {
      sdkSpies.searchWikidataSpy.mockResolvedValue(wrapSdkResponse([]))
      mountDialog("test")
      await flushPromises()
      expect(wikidataModal()?.textContent).toContain(
        "No Wikidata entries found"
      )
    })

    it("displays search results in select when searchKey is provided", async () => {
      mockWikidataSearchResult(sdkSpies.searchWikidataSpy, "Dog", "Q11399")
      mountDialog("dog")
      await flushPromises()
      expect(wikidataSearchResults().textContent).toContain("Dog")
      expect(wikidataSearchResultItem("Q11399")).toBeTruthy()
    })
  })

  describe("input handling", () => {
    it("emits update:modelValue when user types in the input", async () => {
      const wrapper = mountDialog("Test Title")
      await flushPromises()
      const input = wikidataInput()
      input.value = "Q456"
      input.dispatchEvent(new Event("input", { bubbles: true }))
      await flushPromises()
      expect(wrapper.emitted("update:modelValue")?.[0]).toEqual(["Q456"])
    })

    it("allows manual input of Wikidata ID", async () => {
      const wrapper = mountDialog("test")
      await flushPromises()
      const input = wikidataInput()
      expect(sdkSpies.searchWikidataSpy).toHaveBeenCalled()
      input.value = "Q999"
      input.dispatchEvent(new Event("input", { bubbles: true }))
      await flushPromises()
      expect(wrapper.emitted("update:modelValue")?.[0]).toEqual(["Q999"])
    })
  })

  describe("title matching and actions", () => {
    it("emits selected with no titleAction when titles match", async () => {
      const searchResult = mockWikidataSearchResult(
        sdkSpies.searchWikidataSpy,
        "dog",
        "Q11399"
      )
      const wrapper = mountDialog("dog")
      await flushPromises()
      await clickWikidataSearchResult("Q11399")
      const emitted = wrapper.emitted("selected")?.[0]
      expect(emitted?.[0]).toEqual(searchResult)
      expect(emitted?.[1]).toBeUndefined()
      expect(wikidataInput().value).toBe("Q11399")
    })

    it("emits selected with no titleAction when titles match case-insensitively", async () => {
      const searchResult = mockWikidataSearchResult(
        sdkSpies.searchWikidataSpy,
        "Dog",
        "Q11399"
      )
      const wrapper = mountDialog("DOG")
      await flushPromises()
      await clickWikidataSearchResult("Q11399")
      const emitted = wrapper.emitted("selected")?.[0]
      expect(emitted?.[0]).toEqual(searchResult)
      expect(emitted?.[1]).toBeUndefined()
    })

    it("shows replace title and add alias controls when suggested title differs", async () => {
      mockWikidataSearchResult(sdkSpies.searchWikidataSpy, "Canine", "Q11399")
      mountDialog("dog")
      await flushPromises()
      await clickWikidataSearchResult("Q11399")
      expectReplaceTitleAndAddAliasControls("Canine")
    })

    it.each([false, true])(
      "emits selected with replace action when showSaveButton is %s",
      async (showSaveButton) => {
        const { wrapper, searchResult } = await mountWikidataDialogReady({
          searchWikidataSpy: sdkSpies.searchWikidataSpy,
          searchKey: "dog",
          searchLabel: "Canine",
          wikidataId: "Q11399",
          mountOptions: showSaveButton ? { showSaveButton: true } : undefined,
        })
        await selectWikidataSearchResultWithTitleAction("Q11399", "Replace")
        const emitted = wrapper.emitted("selected")?.[0]
        expect(emitted?.[0]).toEqual(searchResult)
        expect(emitted?.[1]).toBe("replace")
      }
    )

    it.each([false, true])(
      "emits selected with add alias action when showSaveButton is %s",
      async (showSaveButton) => {
        const { wrapper, searchResult } = await mountWikidataDialogReady({
          searchWikidataSpy: sdkSpies.searchWikidataSpy,
          searchKey: "dog",
          searchLabel: "Canine",
          wikidataId: "Q11399",
          mountOptions: showSaveButton ? { showSaveButton: true } : undefined,
        })
        await selectWikidataSearchResultWithTitleAction("Q11399", "Append")
        const emitted = wrapper.emitted("selected")?.[0]
        expect(emitted?.[0]).toEqual(searchResult)
        expect(emitted?.[1]).toBe("append")
      }
    )
  })

  describe("open link button", () => {
    it("shows open link button when Wikidata ID is present and showSaveButton is true", async () => {
      mountDialog("Test Title", { modelValue: "Q123", showSaveButton: true })
      await flushPromises()
      const openLinkButton = wikidataModal()?.querySelector(
        'button[title="open link"]'
      ) as HTMLButtonElement
      expect(openLinkButton).toBeTruthy()
      expect(openLinkButton?.querySelector("svg")).toBeTruthy()
    })

    it("shows open link button when showSaveButton is false but Wikidata ID is present", async () => {
      mountDialog("Test Title", { modelValue: "Q123", showSaveButton: false })
      await flushPromises()
      const openLinkButton = wikidataModal()?.querySelector(
        'button[title="open link"]'
      ) as HTMLButtonElement
      expect(openLinkButton).toBeTruthy()
      expect(openLinkButton?.querySelector("svg")).toBeTruthy()
    })

    it("hides open link button when Wikidata ID is empty", async () => {
      mountDialog("Test Title", { modelValue: "", showSaveButton: true })
      await flushPromises()
      const openLinkButton = wikidataModal()?.querySelector(
        'button[title="open link"]'
      ) as HTMLButtonElement
      expect(openLinkButton).toBeTruthy()
      expect(openLinkButton.style.display).toBe("none")
    })

    it("opens Wikipedia URL when available", async () => {
      const wikipediaUrl = "https://en.wikipedia.org/wiki/Test"
      sdkSpies.fetchWikidataEntitySpy.mockResolvedValue(
        wrapSdkResponse({
          WikipediaEnglishUrl: wikipediaUrl,
          // biome-ignore lint/suspicious/noExplicitAny: SDK response types are complex unions that require any for proper mocking
        } as any)
      )
      const windowOpenSpy = vi.spyOn(window, "open").mockImplementation(
        () =>
          ({
            location: { href: "" },
            focus: vi.fn(),
          }) as unknown as Window
      )
      mountDialog("Test Title", { modelValue: "Q123", showSaveButton: true })
      await flushPromises()
      const openLinkButton = wikidataModal()?.querySelector(
        'button[title="open link"]'
      ) as HTMLButtonElement
      openLinkButton.click()
      await flushPromises()
      expect(windowOpenSpy).toHaveBeenCalledWith("")
      expect(sdkSpies.fetchWikidataEntitySpy).toHaveBeenCalledWith({
        path: { wikidataId: "Q123" },
      })
      windowOpenSpy.mockRestore()
    })

    it("opens Wikidata URL when Wikipedia URL is not available", async () => {
      sdkSpies.fetchWikidataEntitySpy.mockResolvedValue(
        wrapSdkResponse({
          WikipediaEnglishUrl: "",
          // biome-ignore lint/suspicious/noExplicitAny: SDK response types are complex unions that require any for proper mocking
        } as any)
      )
      const windowOpenSpy = vi.spyOn(window, "open").mockImplementation(
        () =>
          ({
            location: { href: "" },
            focus: vi.fn(),
          }) as unknown as Window
      )
      mountDialog("Test Title", { modelValue: "Q123", showSaveButton: true })
      await flushPromises()
      const openLinkButton = wikidataModal()?.querySelector(
        'button[title="open link"]'
      ) as HTMLButtonElement
      openLinkButton.click()
      await flushPromises()
      expect(windowOpenSpy).toHaveBeenCalledWith("")
      expect(sdkSpies.fetchWikidataEntitySpy).toHaveBeenCalledWith({
        path: { wikidataId: "Q123" },
      })
      windowOpenSpy.mockRestore()
    })
  })

  describe("edit mode with showSaveButton", () => {
    it("does not auto-save when selecting from result list if showSaveButton is true", async () => {
      mockWikidataSearchResult(sdkSpies.searchWikidataSpy, "dog", "Q11399")
      const wrapper = mountDialog("dog", { showSaveButton: true })
      await flushPromises()
      await clickWikidataSearchResult("Q11399")
      expect(wrapper.emitted("selected")).toBeFalsy()
      expect(wrapper.emitted("update:modelValue")?.[0]).toEqual(["Q11399"])
      expect(wikidataSaveButton()).toBeTruthy()
    })

    it("saves when clicking Save button after selecting from result list", async () => {
      mockWikidataSearchResult(sdkSpies.searchWikidataSpy, "dog", "Q11399")
      const wrapper = mountDialog("dog", { showSaveButton: true })
      await flushPromises()
      await clickWikidataSearchResult("Q11399")
      wikidataSaveButton().click()
      await flushPromises()
      expect(wrapper.emitted("save")?.[0]).toEqual(["Q11399"])
    })

    it("shows replace title and add alias controls when selecting result with different title and showSaveButton is true", async () => {
      mockWikidataSearchResult(sdkSpies.searchWikidataSpy, "Canine", "Q11399")
      mountDialog("dog", { showSaveButton: true })
      await flushPromises()
      await clickWikidataSearchResult("Q11399")
      expectReplaceTitleAndAddAliasControls("Canine")
    })

    it("enables Save and emits save with empty string when clearing and canSaveEmptyToClear", async () => {
      const wrapper = mountDialog("dog", {
        showSaveButton: true,
        canSaveEmptyToClear: true,
        savedValue: "Q123",
        modelValue: "Q123",
      })
      await flushPromises()
      const input = wikidataInput()
      expect(input.value).toBe("Q123")
      input.value = ""
      input.dispatchEvent(new Event("input", { bubbles: true }))
      await flushPromises()
      const saveButton = wikidataSaveButton()
      expect(saveButton.disabled).toBe(false)
      saveButton.click()
      await flushPromises()
      expect(wrapper.emitted("save")?.[0]).toEqual([""])
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

  describe("append alias to frontmatter", () => {
    it("writes a YAML aliases list instead of appending to the title", () => {
      const result = appendAliasToNoteContent("## Workshop\n", "Canine")
      expect(result).toBe(`---\naliases:\n  - Canine\n---\n## Workshop\n`)
    })

    it("preserves existing frontmatter when adding the first aliases list", () => {
      const markdown = `---
wikidata_id: Q11399
---

# Body`
      const result = appendAliasToNoteContent(markdown, "Canine")
      expect(result).toContain("wikidata_id: Q11399")
      expect(result).toContain("aliases:\n  - Canine")
      expect(result).toContain("# Body")
    })

    it("merges a new alias into an existing aliases list", () => {
      const markdown = `---
aliases:
  - puppy
---

# Body`
      const result = appendAliasToNoteContent(markdown, "Canine")
      expect(result).toBe(`---
aliases:
  - puppy
  - Canine
---

# Body`)
    })

    it("dedupes by normalized lookup key when merging", () => {
      const markdown = `---
aliases:
  - Puppy
---

# Body`
      expect(appendAliasToNoteContent(markdown, "puppy")).toBeNull()
    })

    it("dedupes NFKC-normalized aliases when merging", () => {
      const markdown = `---
aliases:
  - Ｃａｎｉｎｅ
---

# Body`
      expect(appendAliasToNoteContent(markdown, "Canine")).toBeNull()
    })

    it("returns null when aliases is not a YAML list", () => {
      const markdown = `---
aliases: puppy
---

# Body`
      expect(appendAliasToNoteContent(markdown, "Canine")).toBeNull()
    })
  })
})
