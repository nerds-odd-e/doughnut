import { useBookReadingBootstrap } from "@/composables/useBookReadingBootstrap"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { wrapSdkResponse } from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
import { flushPromises, mount } from "@vue/test-utils"
import { defineComponent } from "vue"
import { beforeEach, describe, expect, it, vi } from "vitest"
import epubMinimalUrl from "../../../e2e_test/fixtures/book_reading/epub_valid_minimal.epub?url"
import topMathsUrl from "../../../e2e_test/fixtures/book_reading/top-maths.pdf?url"

describe("useBookReadingBootstrap", () => {
  const notebookId = 7
  let topMathsPdfBytes!: ArrayBuffer
  let epubMinimalBytes!: ArrayBuffer

  beforeEach(async () => {
    vi.restoreAllMocks()
    if (!topMathsPdfBytes) {
      const res = await fetch(topMathsUrl)
      topMathsPdfBytes = await res.arrayBuffer()
      const epubRes = await fetch(epubMinimalUrl)
      epubMinimalBytes = await epubRes.arrayBuffer()
    }
  })

  function mountBootstrap() {
    const Root = defineComponent({
      props: { nid: { type: Number, required: true } },
      setup(props: { nid: number }) {
        return useBookReadingBootstrap(props.nid)
      },
      template: "<div />",
    })
    return mount(Root, { props: { nid: notebookId } })
  }

  it("sets pdf bootstrap with initial last-read when position includes a PDF locator", async () => {
    vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
      wrapSdkResponse(
        makeMe.aBook.notebookId(String(notebookId)).please()
      ) as Awaited<ReturnType<typeof NotebookBooksController.getBook>>
    )
    vi.spyOn(
      NotebookBooksController,
      "getNotebookBookReadingPosition"
    ).mockResolvedValue(
      wrapSdkResponse({
        id: 1,
        locator: {
          type: "PdfLocator_Full",
          pageIndex: 2,
          bbox: [0, 750, 100, 600],
          contentBlockId: null,
          derivedTitle: null,
        },
        selectedBookBlockId: 42,
      }) as Awaited<
        ReturnType<
          typeof NotebookBooksController.getNotebookBookReadingPosition
        >
      >
    )
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(topMathsPdfBytes.slice(0), {
        status: 200,
        headers: { "Content-Type": "application/pdf" },
      })
    )

    const w = mountBootstrap()
    await flushPromises()

    const vm = w.vm as {
      bootstrap: {
        kind: string
        initialLastRead: { pageIndexZeroBased: number; normalizedY: number }
        initialSelectedBlockId: number | null
      } | null
    }
    await vi.waitFor(() => expect(vm.bootstrap).not.toBeNull())
    expect(vm.bootstrap?.kind).toBe("pdf")
    expect(vm.bootstrap?.initialLastRead).toEqual({
      pageIndexZeroBased: 2,
      normalizedY: 750,
    })
    expect(vm.bootstrap?.initialSelectedBlockId).toBe(42)
  })

  it("sets epub bootstrap with null initial locator when no reading position", async () => {
    vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
      wrapSdkResponse(
        makeMe.aBook
          .notebookId(String(notebookId))
          .format("epub")
          .blocks([])
          .please()
      ) as Awaited<ReturnType<typeof NotebookBooksController.getBook>>
    )
    vi.spyOn(
      NotebookBooksController,
      "getNotebookBookReadingPosition"
    ).mockResolvedValue(
      wrapSdkResponse(undefined) as Awaited<
        ReturnType<
          typeof NotebookBooksController.getNotebookBookReadingPosition
        >
      >
    )
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(epubMinimalBytes.slice(0), {
        status: 200,
        headers: { "Content-Type": "application/epub+zip" },
      })
    )

    const w = mountBootstrap()
    await flushPromises()

    const vm = w.vm as {
      bootstrap: {
        kind: string
        initialLocator: unknown
        initialSelectedBlockId: number | null
      } | null
    }
    await vi.waitFor(() => expect(vm.bootstrap).not.toBeNull())
    expect(vm.bootstrap?.kind).toBe("epub")
    expect(vm.bootstrap?.initialLocator).toBeNull()
    expect(vm.bootstrap?.initialSelectedBlockId).toBeNull()
  })

  it("sets file error and leaves bootstrap null when file fetch is not ok", async () => {
    vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
      wrapSdkResponse(
        makeMe.aBook.notebookId(String(notebookId)).please()
      ) as Awaited<ReturnType<typeof NotebookBooksController.getBook>>
    )
    vi.spyOn(
      NotebookBooksController,
      "getNotebookBookReadingPosition"
    ).mockResolvedValue(
      wrapSdkResponse(undefined) as Awaited<
        ReturnType<
          typeof NotebookBooksController.getNotebookBookReadingPosition
        >
      >
    )
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(null, { status: 404 })
    )

    const w = mountBootstrap()
    await flushPromises()

    const vm = w.vm as {
      bootstrap: unknown
      fileError: string | null
      fileLoading: boolean
    }
    await vi.waitFor(() => expect(vm.fileLoading).toBe(false))
    expect(vm.fileError).toBe("Could not load the book file.")
    expect(vm.bootstrap).toBeNull()
  })
})
