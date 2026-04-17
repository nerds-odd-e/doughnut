/**
 * pdf.js 5.x uses `Uint8Array.prototype.toHex` for document fingerprints. Some runtimes
 * (including Cypress’s Electron worker realm) omit it; the worker must define it before pdf.worker runs.
 */
if (!("toHex" in Uint8Array.prototype)) {
  Object.defineProperty(Uint8Array.prototype, "toHex", {
    value: function toHex(this: Uint8Array): string {
      let hex = ""
      for (let i = 0; i < this.length; i++) {
        hex += this[i]!.toString(16).padStart(2, "0")
      }
      return hex
    },
    configurable: true,
    writable: true,
  })
}

export {}
