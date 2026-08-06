export const emptyKey = {}

export const highlightOnly = { kind: 'highlight-only' as const }

export const slashAndNumber = (choiceCount: number) => ({
  kind: 'slash-and-number-or-highlight' as const,
  choiceCount,
})

export const filterBuffer = { kind: 'filter-buffer' as const }
