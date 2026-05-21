import { watch, type Ref } from "vue"

/**
 * Syncs a boolean open state with a native `<dialog class="daisy-modal">`.
 * daisyUI v5 styles open modals via `.modal-open`, `[open]`, etc., but the browser
 * keeps `display: none` on `<dialog>` until showModal() runs.
 */
export function useDaisyDialog(
  isOpen: Ref<boolean>,
  dialogRef: Ref<HTMLDialogElement | null>
) {
  watch(
    [isOpen, dialogRef],
    ([open, dialog]) => {
      if (!dialog) return
      try {
        if (open) {
          if (!dialog.open) dialog.showModal()
        } else if (dialog.open) {
          dialog.close()
        }
      } catch {
        // Teleport stubs in unit tests may not implement dialog APIs
      }
    },
    { flush: "post" }
  )
}
