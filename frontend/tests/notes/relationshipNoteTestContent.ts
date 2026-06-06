/** Relationship note markdown for frontend tests. */
export function relationshipNoteContent(
  relationKebab: string,
  sourceLink: string,
  targetLink: string
): string {
  return `---
type: relationship
relation: ${relationKebab}
source: "${sourceLink.replace(/"/g, '\\"')}"
target: "${targetLink.replace(/"/g, '\\"')}"
---
`
}
