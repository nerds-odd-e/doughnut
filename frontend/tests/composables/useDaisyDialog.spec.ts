import { describe, it, expect, vi, beforeEach } from "vitest"
import { ref, nextTick } from "vue"
import { useDaisyDialog } from "@/composables/useDaisyDialog"

describe("useDaisyDialog", () => {
  beforeEach(() => {
    HTMLDialogElement.prototype.showModal = vi.fn()
    HTMLDialogElement.prototype.close = vi.fn()
  })

  it("calls showModal when isOpen becomes true", async () => {
    const isOpen = ref(false)
    const dialog = document.createElement("dialog")
    Object.defineProperty(dialog, "open", { value: false, writable: true })
    const dialogRef = ref(dialog)
    useDaisyDialog(isOpen, dialogRef)

    isOpen.value = true
    await nextTick()

    expect(dialog.showModal).toHaveBeenCalled()
  })

  it("calls close when isOpen becomes false", async () => {
    const isOpen = ref(true)
    const dialog = document.createElement("dialog")
    Object.defineProperty(dialog, "open", { value: true, writable: true })
    const dialogRef = ref(dialog)
    useDaisyDialog(isOpen, dialogRef)

    isOpen.value = false
    await nextTick()

    expect(dialog.close).toHaveBeenCalled()
  })
})
