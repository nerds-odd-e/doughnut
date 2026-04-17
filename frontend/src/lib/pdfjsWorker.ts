import "./pdfJsMapGetOrInsertComputedPolyfill"
import { GlobalWorkerOptions } from "pdfjs-dist/build/pdf.mjs"
// Worker entry polyfills `Uint8Array.prototype.toHex` then loads the stock pdf.worker (see pdfWorkerEntry.ts).
import workerUrl from "./pdfWorkerEntry.ts?url"

GlobalWorkerOptions.workerSrc = workerUrl
