import {
  cyFetchOpenAiImposterRequests,
  responsesPostBodies,
  type RecordedImposterRequest,
} from './openAiImposterRecordedRequests'

const PROPERTY_FOCUS_POST_POLL_MS = 250
const PROPERTY_FOCUS_POST_POLL_ATTEMPTS = 40

const PROPERTY_FOCUS_INSTRUCTION_HEADER =
  'Focus on one property of the focus note:'

export const propertyFocusQuestionGenerationPostBodies = (
  requests: RecordedImposterRequest[]
): string[] =>
  responsesPostBodies(requests).filter((b) =>
    b.includes(PROPERTY_FOCUS_INSTRUCTION_HEADER)
  )

const instructionsFromResponsesPostBodies = (bodies: string[]): string[] =>
  bodies.flatMap((body) => {
    try {
      const parsed = JSON.parse(body) as { instructions?: string }
      return parsed.instructions ? [parsed.instructions] : []
    } catch {
      return []
    }
  })

export const assertPropertyFocusInstructionInPostBodies = (
  bodies: string[],
  propertyKey: string,
  propertyValue: string
) => {
  expect(
    bodies.length,
    [
      'Property focus recall E2E: expected at least one OpenAI Responses POST body',
      'whose instructions include the property-focus block.',
      'Fewer bodies usually means recall did not trigger question generation,',
      'Mountebank stubs did not match, or the assertion ran before eager-fetch finished.',
      `Found ${bodies.length} matching body/bodies.`,
    ].join('\n')
  ).to.be.at.least(1)

  const instructions = instructionsFromResponsesPostBodies(bodies)
  expect(
    instructions.length,
    'Property focus recall E2E: could not parse instructions from matching POST bodies'
  ).to.be.at.least(1)

  const combined = instructions.join('\n')
  expect(
    combined,
    `Property focus recall E2E: instructions should name property "${propertyKey}"`
  ).to.include(`Focus on property "${propertyKey}"`)
  expect(
    combined,
    'Property focus recall E2E: instructions should include property key'
  ).to.include(`Property key: ${propertyKey}`)
  expect(
    combined,
    `Property focus recall E2E: instructions should include property value "${propertyValue}"`
  ).to.include(`Property value: ${propertyValue}`)
}

const pollPropertyFocusQuestionGenerationPostBodies = (
  propertyKey: string,
  propertyValue: string,
  attempt = 0
): void => {
  cyFetchOpenAiImposterRequests().then((requests) => {
    const bodies = propertyFocusQuestionGenerationPostBodies(requests)
    if (bodies.length >= 1 || attempt >= PROPERTY_FOCUS_POST_POLL_ATTEMPTS) {
      assertPropertyFocusInstructionInPostBodies(
        bodies,
        propertyKey,
        propertyValue
      )
      return
    }
    cy.wait(PROPERTY_FOCUS_POST_POLL_MS)
    pollPropertyFocusQuestionGenerationPostBodies(
      propertyKey,
      propertyValue,
      attempt + 1
    )
  })
}

export const pollUntilPropertyFocusInstructionMatches = (
  propertyKey: string,
  propertyValue: string
): void => {
  pollPropertyFocusQuestionGenerationPostBodies(propertyKey, propertyValue)
}
