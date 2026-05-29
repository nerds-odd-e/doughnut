export type NavigationActionItemName = "resumeRecall" | "assimilate"

export function isNavigationActionItem(
  name?: string
): name is NavigationActionItemName {
  return name === "resumeRecall" || name === "assimilate"
}
