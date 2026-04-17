import { epubDisplayHref } from "@/lib/book-reading/asEpubLocator"
import type {
  BookFull,
  BookUserLastReadPosition,
  ContentLocatorFull,
  EpubLocatorFull,
  PdfLocatorFull,
} from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { onMounted, ref, type Ref } from "vue"

export type BookReadingBootstrap =
  | {
      kind: "pdf"
      book: BookFull
      bytes: ArrayBuffer
      initialLastRead: {
        pageIndexZeroBased: number
        normalizedY: number
      } | null
      initialSelectedBlockId: number | null
    }
  | {
      kind: "epub"
      book: BookFull
      bytes: ArrayBuffer
      initialLocator: ContentLocatorFull | null
      initialSelectedBlockId: number | null
    }

function notebookBookFilePath(notebookId: number) {
  return `/api/notebooks/${notebookId}/book/file`
}

function epubInitialLocatorFromSaved(
  loc: ContentLocatorFull | undefined
): ContentLocatorFull | null {
  if (!loc || loc.type !== "EpubLocator_Full") {
    return null
  }
  const epub = loc as EpubLocatorFull
  const s = epubDisplayHref(epub)
  return s.length > 0 ? loc : null
}

function pdfInitialLastReadFromSaved(
  loc: ContentLocatorFull | undefined
): { pageIndexZeroBased: number; normalizedY: number } | null {
  if (!loc || loc.type !== "PdfLocator_Full") {
    return null
  }
  const pdf = loc as PdfLocatorFull
  const y = pdf.bbox?.[1] ?? 0
  return {
    pageIndexZeroBased: pdf.pageIndex,
    normalizedY: Math.round(y),
  }
}

export function useBookReadingBootstrap(notebookId: number) {
  const book: Ref<BookFull | null> = ref(null)
  const fileLoading = ref(false)
  const fileError = ref<string | null>(null)
  const bootstrap: Ref<BookReadingBootstrap | null> = ref(null)

  function mergeBookIntoBootstrap(updated: BookFull) {
    const b = bootstrap.value
    if (!b) {
      return
    }
    bootstrap.value = { ...b, book: updated }
  }

  onMounted(async () => {
    const { data, error } = await NotebookBooksController.getBook({
      path: { notebook: notebookId },
    })
    if (error || !data) {
      return
    }
    book.value = data
    const notebook = Number(data.notebookId)
    fileLoading.value = true
    fileError.value = null
    try {
      const [res, posResult] = await Promise.all([
        fetch(notebookBookFilePath(notebook), {
          credentials: "same-origin",
        }),
        NotebookBooksController.getNotebookBookReadingPosition({
          path: { notebook },
        }).catch(() => null),
      ])
      if (!res.ok) {
        fileError.value = "Could not load the book file."
        return
      }
      const pos: BookUserLastReadPosition | null =
        posResult !== null && !posResult.error && posResult.data
          ? posResult.data
          : null
      const initialSelectedBlockId =
        typeof pos?.selectedBookBlockId === "number"
          ? pos.selectedBookBlockId
          : null
      const loc = pos?.locator as ContentLocatorFull | undefined
      const bytes = await res.arrayBuffer()
      if (data.format === "epub") {
        bootstrap.value = {
          kind: "epub",
          book: data,
          bytes,
          initialLocator: epubInitialLocatorFromSaved(loc),
          initialSelectedBlockId,
        }
      } else {
        bootstrap.value = {
          kind: "pdf",
          book: data,
          bytes,
          initialLastRead: pdfInitialLastReadFromSaved(loc),
          initialSelectedBlockId,
        }
      }
    } catch {
      fileError.value = "Could not load the book file."
    } finally {
      fileLoading.value = false
    }
  })

  return {
    book,
    fileLoading,
    fileError,
    bootstrap,
    mergeBookIntoBootstrap,
  }
}
