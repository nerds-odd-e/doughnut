import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"
import {
  fakeEpubBytes,
  fakePdfBytes,
  mockFetchBytes,
  mockFetchNotFound,
  mockGetBookEpub,
  mockGetBookPdf,
  mockNoReadingPosition,
  mockReadingPositionWithPdfLocator,
  mountBootstrap,
  waitForBootstrap,
} from "./useBookReadingBootstrapTestSupport"

describe("useBookReadingBootstrap", () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it("sets pdf bootstrap with initial last-read when position includes a PDF locator", async () => {
    mockGetBookPdf()
    mockReadingPositionWithPdfLocator()
    mockFetchBytes(fakePdfBytes, "application/pdf")

    const wrapper = mountBootstrap()
    const bootstrap = await waitForBootstrap<{
      kind: string
      initialLastRead: { pageIndexZeroBased: number; normalizedY: number }
      initialSelectedBlockId: number | null
    }>(wrapper)

    expect(bootstrap.kind).toBe("pdf")
    expect(bootstrap.initialLastRead).toEqual({
      pageIndexZeroBased: 2,
      normalizedY: 750,
    })
    expect(bootstrap.initialSelectedBlockId).toBe(42)
  })

  it("sets epub bootstrap with null initial locator when no reading position", async () => {
    mockGetBookEpub()
    mockNoReadingPosition()
    mockFetchBytes(fakeEpubBytes, "application/epub+zip")

    const wrapper = mountBootstrap()
    const bootstrap = await waitForBootstrap<{
      kind: string
      initialLocator: unknown
      initialSelectedBlockId: number | null
    }>(wrapper)

    expect(bootstrap.kind).toBe("epub")
    expect(bootstrap.initialLocator).toBeNull()
    expect(bootstrap.initialSelectedBlockId).toBeNull()
  })

  it("sets file error and leaves bootstrap null when file fetch is not ok", async () => {
    mockGetBookPdf()
    mockNoReadingPosition()
    mockFetchNotFound()

    const wrapper = mountBootstrap()
    await flushPromises()

    const vm = wrapper.vm as {
      bootstrap: unknown
      fileError: string | null
      fileLoading: boolean
    }
    expect(vm.fileLoading).toBe(false)
    expect(vm.fileError).toBe("Could not load the book file.")
    expect(vm.bootstrap).toBeNull()
  })
})
