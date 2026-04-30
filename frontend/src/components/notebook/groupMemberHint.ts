import type { NotebookClientView } from "@generated/doughnut-backend-api"

export function groupCatalogMemberPreviewHint(args: {
  groupName: string
  notebooks: NotebookClientView[]
  memberPreviewLimit: number | null
  catalogFilterActive: boolean
}): {
  subtitle: string
  ariaLabel: string
} {
  const { groupName, notebooks, memberPreviewLimit, catalogFilterActive } = args
  const total = notebooks.length
  if (total === 0) {
    return {
      subtitle: "No notebooks in this group",
      ariaLabel: `${groupName} notebook group with no notebooks`,
    }
  }

  const limit = memberPreviewLimit
  const shown = limit === null ? total : Math.min(limit, total)
  const matching = catalogFilterActive

  let subtitle: string
  if (limit !== null && total > limit) {
    subtitle = matching
      ? `Showing ${shown} of ${total} matching notebooks`
      : `Showing ${shown} of ${total} notebooks`
  } else if (matching) {
    subtitle =
      total === 1 ? "1 matching notebook" : `${total} matching notebooks`
  } else {
    subtitle = total === 1 ? "1 notebook" : `${total} notebooks`
  }

  const names = notebooks.map((nb) => nb.notebook.name ?? "Untitled")
  const namesForAria =
    names.length <= 12
      ? names.join(", ")
      : `${names.slice(0, 12).join(", ")}, and ${names.length - 12} more`
  const ariaLabel = `${groupName} notebook group. ${subtitle}. Members: ${namesForAria}.`

  return { subtitle, ariaLabel }
}
