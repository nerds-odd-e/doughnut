import type { Notebook } from "@generated/doughnut-backend-api"

export function groupMemberHint(notebooks: Notebook[]): {
  subtitle: string
  ariaLabel: string
} {
  const n = notebooks.length
  if (n === 0) {
    return {
      subtitle: "No notebooks in this group",
      ariaLabel: "Notebook group with no notebooks",
    }
  }
  const titles = notebooks.map((nb) => nb.title ?? "Untitled")
  const ariaLabel = `Notebook group: ${titles.join(", ")}`
  if (n === 1) {
    return { subtitle: titles[0]!, ariaLabel }
  }
  if (n === 2) {
    return { subtitle: `${titles[0]}, ${titles[1]}`, ariaLabel }
  }
  if (n === 3) {
    return {
      subtitle: `${titles[0]}, ${titles[1]}, ${titles[2]}`,
      ariaLabel,
    }
  }
  return {
    subtitle: `${titles[0]}, ${titles[1]}, and ${n - 2} more`,
    ariaLabel,
  }
}
