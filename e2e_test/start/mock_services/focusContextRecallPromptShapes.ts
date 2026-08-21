/**
 * Single source for the three focus-context recall E2E prompt-shape discriminators
 * (Mountebank stubs and post-hoc assertions must stay aligned).
 */

export const focusContextRecallStubUserContent = {
  depthTwoWiki:
    '[\\s\\S]*Title: FarDepthTwo[\\s\\S]*Path:[\\s\\S]*->[\\s\\S]*->[\\s\\S]*',
  folderSiblings:
    '[\\s\\S]*Title: FocusFolder[\\s\\S]*Title: SibOne[\\s\\S]*Title: SibTwo[\\s\\S]*',
  wikiLinkedBahamas:
    '[\\s\\S]*Title: WikiRecall[\\s\\S]*Title: Bahamas[\\s\\S]*',
} as const

const pathHasTwoArrows = (body: string): boolean =>
  /Path:[^\n]*->[^\n]*->/.test(body)

export const focusContextRecallPromptMatches = {
  wikiLinkedBahamas: (body: string): boolean =>
    body.includes('Title: WikiRecall') && body.includes('Title: Bahamas'),
  depthTwoWiki: (body: string): boolean =>
    body.includes('Title: FarDepthTwo') && pathHasTwoArrows(body),
  folderSiblings: (body: string): boolean =>
    body.includes('Title: FocusFolder') &&
    body.includes('Title: SibOne') &&
    body.includes('Title: SibTwo'),
}

export const focusContextRecallPromptBodyHints = (bodies: string[]): string =>
  bodies
    .map((body, i) =>
      [
        `--- body[${i}] (chars=${body.length}) ---`,
        `  Title:WikiRecall: ${body.includes('Title: WikiRecall')}`,
        `  Title:Bahamas: ${body.includes('Title: Bahamas')}`,
        `  Title:FarDepthTwo: ${body.includes('Title: FarDepthTwo')}`,
        `  Path with two ->: ${pathHasTwoArrows(body)}`,
        `  Title:FocusFolder: ${body.includes('Title: FocusFolder')}`,
        `  Title:SibOne: ${body.includes('Title: SibOne')}`,
        `  Title:SibTwo: ${body.includes('Title: SibTwo')}`,
      ].join('\n')
    )
    .join('\n')
