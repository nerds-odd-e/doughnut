import { flushPromises } from "@vue/test-utils"
import { wrapSdkResponse } from "@tests/helpers"
import {
  useWikidataAssociationDialogTestLifecycle,
  wikidataModal,
} from "@tests/notes/wikidataAssociationDialogTestSupport"
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

function openLinkButton(): HTMLButtonElement | null {
  return wikidataModal()?.querySelector(
    'button[title="open link"]'
  ) as HTMLButtonElement | null
}

function spyBrowsePopup() {
  const popup = {
    location: { href: "" },
    focus: vi.fn(),
  }
  const windowOpenSpy = vi
    .spyOn(window, "open")
    .mockReturnValue(popup as unknown as Window)
  return { popup, windowOpenSpy }
}

const { mountDialog, getSdkSpies } = useWikidataAssociationDialogTestLifecycle()

describe("WikidataAssociationDialog open link", () => {
  it.each([true, false])(
    "shows open link button when Wikidata ID is present (showSaveButton=%s)",
    async (showSaveButton) => {
      mountDialog("Test Title", { modelValue: "Q123", showSaveButton })
      await flushPromises()
      const button = openLinkButton()
      expect(button).toBeTruthy()
      expect(button?.querySelector("svg")).toBeTruthy()
    }
  )

  it("hides open link button when Wikidata ID is empty", async () => {
    mountDialog("Test Title", { modelValue: "", showSaveButton: true })
    await flushPromises()
    expect(openLinkButton()?.style.display).toBe("none")
  })

  it("opens Wikipedia URL in popup when available", async () => {
    const wikipediaUrl = "https://en.wikipedia.org/wiki/Test"
    getSdkSpies().fetchWikidataEntitySpy.mockResolvedValue(
      wrapSdkResponse({
        WikipediaEnglishUrl: wikipediaUrl,
        // biome-ignore lint/suspicious/noExplicitAny: SDK entity shape
      } as any)
    )
    const { popup, windowOpenSpy } = spyBrowsePopup()
    mountDialog("Test Title", { modelValue: "Q123", showSaveButton: true })
    await flushPromises()
    openLinkButton()!.click()
    await flushPromises()
    expect(windowOpenSpy).toHaveBeenCalledWith("")
    expect(popup.location.href).toBe(wikipediaUrl)
    expect(getSdkSpies().fetchWikidataEntitySpy).toHaveBeenCalledWith({
      path: { wikidataId: "Q123" },
    })
    windowOpenSpy.mockRestore()
  })

  it("opens Wikidata URL in popup when Wikipedia URL is absent", async () => {
    getSdkSpies().fetchWikidataEntitySpy.mockResolvedValue(
      wrapSdkResponse({
        WikipediaEnglishUrl: "",
        // biome-ignore lint/suspicious/noExplicitAny: SDK entity shape
      } as any)
    )
    const { popup, windowOpenSpy } = spyBrowsePopup()
    mountDialog("Test Title", { modelValue: "Q123", showSaveButton: true })
    await flushPromises()
    openLinkButton()!.click()
    await flushPromises()
    expect(popup.location.href).toBe("https://www.wikidata.org/wiki/Q123")
    windowOpenSpy.mockRestore()
  })
})
