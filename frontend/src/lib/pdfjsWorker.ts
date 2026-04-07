import { GlobalWorkerOptions } from "pdfjs-dist/legacy/build/pdf.mjs"
// Legacy bundle polyfills `Uint8Array.prototype.toHex` (used for PDF fingerprints). The modern
// worker assumes a newer Chromium; Cypress Electron hits "hashOriginal.toHex is not a function".
// Worker and main-thread API must use the same legacy build.
import workerUrl from "pdfjs-dist/legacy/build/pdf.worker.mjs?url"

GlobalWorkerOptions.workerSrc = workerUrl
