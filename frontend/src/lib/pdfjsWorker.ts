import "./pdfJsMapGetOrInsertComputedPolyfill"
import {
  getDocument,
  GlobalWorkerOptions,
  type PDFDocumentProxy,
} from "pdfjs-dist/build/pdf.mjs"
// Use Vite's worker pipeline (`?worker&url`) so the bundle is a real `http(s):` module URL. Plain `?url` can
// inline as `data:` in CI; Cypress/Electron then fails dynamic imports inside the worker (wrong MIME / base URL).
import pdfWorkerEntryUrl from "./pdfWorkerEntry.ts?worker&url"

GlobalWorkerOptions.workerSrc = pdfWorkerEntryUrl

export { getDocument, GlobalWorkerOptions, type PDFDocumentProxy }
