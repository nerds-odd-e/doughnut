import {
  cyFetchOpenAiImposterRequests,
  postRequestBodies,
  type RecordedImposterRequest,
} from './openAiImposterRecordedRequests'

const FOCUS_CONTEXT_POST_POLL_MS = 250
const FOCUS_CONTEXT_POST_POLL_ATTEMPTS = 40

const focusContextQuestionGenerationPostBodies = (
  requests: RecordedImposterRequest[]
): string[] =>
  postRequestBodies(requests).filter(
    (b) => b.includes('Focus Context') && b.includes('"input"')
  )

const assertFocusContextRetrievalPromptShapes = (bodies: string[]) => {
  expect(
    bodies.length,
    [
      'Focus context recall E2E: expected at least 3 OpenAI Responses POST bodies',
      'that include the rendered focus block (# Focus Context) and JSON "input".',
      'Recall eager-fetches one question per due memory tracker, so fewer bodies usually means',
      'fewer assimilated notes, failed requests, Mountebank stubs not matching, or',
      'the assertion ran before eager-fetch finished (increase poll attempts if flaky).',
      `Found ${bodies.length} matching body/bodies.`,
    ].join('\n')
  ).to.be.at.least(3)

  const wikiLinked = bodies.some(
    (b) =>
      b.includes('Title: WikiRecall') &&
      b.includes('Title: Bahamas') &&
      b.includes('Reached by: OutgoingWikiLink')
  )
  const depthTwoWiki = bodies.some(
    (b) =>
      b.includes('Title: FarDepthTwo') &&
      b.includes('Path:') &&
      b.includes('->') &&
      b.includes('Reached by: OutgoingWikiLink')
  )
  const folderSiblings = bodies.some((b) => {
    const matches = b.match(/Reached by: FolderSibling/g)
    return matches !== null && matches.length >= 2
  })

  const perBodyHints = bodies
    .map((b, i) => {
      const folderSiblingHits = b.match(/Reached by: FolderSibling/g)
      return [
        `--- body[${i}] (chars=${b.length}) ---`,
        `  Title:WikiRecall: ${b.includes('Title: WikiRecall')}`,
        `  Title:Bahamas: ${b.includes('Title: Bahamas')}`,
        `  OutgoingWikiLink: ${b.includes('Reached by: OutgoingWikiLink')}`,
        `  Title:FarDepthTwo: ${b.includes('Title: FarDepthTwo')}`,
        `  Path+arrow: ${b.includes('Path:') && b.includes('->')}`,
        `  FolderSibling count: ${folderSiblingHits?.length ?? 0}`,
      ].join('\n')
    })
    .join('\n')

  expect(
    wikiLinked,
    [
      'Focus context recall E2E — wiki-linked retrieval: no single POST body matched all of:',
      '  "Title: WikiRecall" (focus note title in the feature table),',
      '  "Title: Bahamas" (outlinked note),',
      '  "Reached by: OutgoingWikiLink".',
      'Fix the scenario note titles/content (e.g. [[Bahamas]] on WikiRecall) or update this check if titles changed.',
      perBodyHints,
    ].join('\n')
  ).to.eq(true)

  expect(
    depthTwoWiki,
    [
      'Focus context recall E2E — depth-two wiki path: no POST body matched all of:',
      '  "Title: FarDepthTwo" (leaf note title in the feature table),',
      '  "Path:", "->", and "Reached by: OutgoingWikiLink" (markdown path to depth 2).',
      'If you renamed FarDepthTwo, update this assertion and the Mountebank stub regex in',
      'e2e_test/start/questionGenerationService.ts (addFocusContextShapeMcqStubs).',
      perBodyHints,
    ].join('\n')
  ).to.eq(true)

  expect(
    folderSiblings,
    [
      'Focus context recall E2E — folder siblings: no POST body contained',
      '  "Reached by: FolderSibling" at least twice (two retrieved folder peers).',
      'Fix the scenario: FocusFolder plus two peers in the same Folder column (e.g. peers),',
      'or update this check if retrieval wording changed.',
      perBodyHints,
    ].join('\n')
  ).to.eq(true)
}

const pollFocusContextQuestionGenerationPostBodies = (attempt = 0): void => {
  cyFetchOpenAiImposterRequests().then((requests) => {
    const bodies = focusContextQuestionGenerationPostBodies(requests)
    if (bodies.length >= 3 || attempt >= FOCUS_CONTEXT_POST_POLL_ATTEMPTS) {
      assertFocusContextRetrievalPromptShapes(bodies)
      return
    }
    cy.wait(FOCUS_CONTEXT_POST_POLL_MS)
    pollFocusContextQuestionGenerationPostBodies(attempt + 1)
  })
}

export const pollUntilFocusContextRetrievalPromptShapesMatch = (): void => {
  pollFocusContextQuestionGenerationPostBodies()
}
