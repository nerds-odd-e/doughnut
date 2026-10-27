import {
  handleInputListKeydown,
  isUnmodifiedKeydown,
} from "@/utils/searchDialogKeyboard"

const catalogTargetSelector = "[data-notebook-catalog-target]"

export function handleNotebookCatalogKeydown(
  event: KeyboardEvent,
  container: HTMLElement | null,
  input: HTMLInputElement | null
) {
  const targets = Array.from(
    container?.querySelectorAll<HTMLElement>(catalogTargetSelector) ?? []
  )
  if (handleInputListKeydown(event, { input, targets })) return
  if (
    (event.key !== "ArrowLeft" && event.key !== "ArrowRight") ||
    !isUnmodifiedKeydown(event)
  )
    return

  const active = document.activeElement
  if (!(active instanceof HTMLElement) || !targets.includes(active)) return
  const group = active.closest(".notebook-catalog-group")
  const [header, firstMember] =
    group?.querySelectorAll<HTMLElement>(catalogTargetSelector) ?? []
  const destination =
    event.key === "ArrowRight"
      ? active === header
        ? firstMember
        : undefined
      : active !== header
        ? header
        : undefined
  if (!destination) return
  destination.focus()
  event.preventDefault()
}
