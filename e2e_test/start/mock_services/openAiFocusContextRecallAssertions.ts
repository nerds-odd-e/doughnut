import {
  focusContextRecallPromptBodyHints,
  focusContextRecallPromptMatches,
} from './focusContextRecallPromptShapes'
import {
  cyFetchOpenAiImposterRequests,
  postRequestBodies,
  type RecordedImposterRequest,
} from './openAiImposterRecordedRequests'

const focusContextQuestionGenerationPostBodies = (
  requests: RecordedImposterRequest[]
): string[] =>
  postRequestBodies(requests).filter(
    (b) => b.includes('<focus_context>') && b.includes('"input"')
  )

const assertFocusContextRetrievalPromptShapes = (bodies: string[]) => {
  expect(
    bodies.length,
    [
      'Focus context recall E2E: expected at least 3 OpenAI Responses POST bodies',
      'that include the rendered focus block (<focus_context>) and JSON "input".',
      'Recall eager-fetches one question per due memory tracker, so fewer bodies usually means',
      'fewer assimilated notes, failed requests, or Mountebank stubs not matching.',
      'Wait for all due recall-prompt GETs before asserting so eager-fetch can finish.',
      `Found ${bodies.length} matching body/bodies.`,
    ].join('\n')
  ).to.be.at.least(3)

  const wikiLinked = bodies.some(
    focusContextRecallPromptMatches.wikiLinkedBahamas
  )
  const depthTwoWiki = bodies.some(focusContextRecallPromptMatches.depthTwoWiki)
  const folderSiblings = bodies.some(
    focusContextRecallPromptMatches.folderSiblings
  )

  const perBodyHints = focusContextRecallPromptBodyHints(bodies)

  expect(
    wikiLinked,
    [
      'Focus context recall E2E — wiki-linked retrieval: no single POST body matched all of:',
      '  "Title: WikiRecall" (focus note title in the feature table),',
      '  "Title: Bahamas" (outlinked note).',
      'Fix the scenario note titles/content (e.g. [[Bahamas]] on WikiRecall) or update this check if titles changed.',
      perBodyHints,
    ].join('\n')
  ).to.eq(true)

  expect(
    depthTwoWiki,
    [
      'Focus context recall E2E — depth-two wiki path: no POST body matched all of:',
      '  "Title: FarDepthTwo" (leaf note title in the feature table),',
      '  "Path:" with two "->" (markdown path to depth 2).',
      'If you renamed FarDepthTwo, update focusContextRecallPromptShapes.ts',
      '(stub regex and match predicates stay in one place).',
      perBodyHints,
    ].join('\n')
  ).to.eq(true)

  expect(
    folderSiblings,
    [
      'Focus context recall E2E — folder siblings: no POST body matched all of:',
      '  "Title: FocusFolder" (focus note),',
      '  "Title: SibOne" and "Title: SibTwo" (two retrieved folder peers).',
      'Fix the scenario: FocusFolder plus two peers in the same Folder column (e.g. peers),',
      'or update this check if retrieval wording changed.',
      perBodyHints,
    ].join('\n')
  ).to.eq(true)
}

/** Assert focus-context prompt shapes after recall-prompt GETs for all due trackers have completed. */
export const assertFocusContextRetrievalPromptShapesMatch = (): void => {
  cyFetchOpenAiImposterRequests().then((requests) => {
    assertFocusContextRetrievalPromptShapes(
      focusContextQuestionGenerationPostBodies(requests)
    )
  })
}
