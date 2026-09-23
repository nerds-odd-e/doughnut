import { OPEN_AI_SERVICE_LABEL } from './isolated-openai-mock.mjs'
import { WIKIDATA_SERVICE_LABEL } from './isolated-wikidata-mock.mjs'
import {
  SHARED_MOUNTEBANK_MANAGEMENT_PORT,
  SHARED_OPEN_AI_SERVING_PORT,
  SHARED_WIKIDATA_SERVING_PORT,
} from './isolated-mountebank-mock-ports.mjs'
import { assertPortFreeBeforeMockMutation } from './isolated-mountebank-mock-ownership.mjs'

/**
 * Canonical primary Mountebank management + OpenAI serving ports for a
 * primary-target batch that requires the private OpenAI mock. A foreign
 * listener on either port is refusal — never adoption. Reuses the existing
 * ownership-port check so the refusal message and semantics match the
 * isolated private-mock path.
 */
export async function allocatePrimaryOpenAiMockPorts() {
  const managementPort = SHARED_MOUNTEBANK_MANAGEMENT_PORT
  const servingPort = SHARED_OPEN_AI_SERVING_PORT
  const ownershipOpts = { serviceLabel: OPEN_AI_SERVICE_LABEL }
  await assertPortFreeBeforeMockMutation(
    managementPort,
    'management',
    ownershipOpts
  )
  await assertPortFreeBeforeMockMutation(servingPort, 'serving', ownershipOpts)
  return { managementPort, servingPort }
}

/**
 * Canonical primary Mountebank management + Wikidata serving ports for a
 * primary-target batch that requires the private Wikidata mock. A foreign
 * listener on either port is refusal — never adoption. Reuses the existing
 * ownership-port check so the refusal message and semantics match the
 * isolated private-mock path.
 */
export async function allocatePrimaryWikidataMockPorts() {
  const managementPort = SHARED_MOUNTEBANK_MANAGEMENT_PORT
  const servingPort = SHARED_WIKIDATA_SERVING_PORT
  const ownershipOpts = { serviceLabel: WIKIDATA_SERVICE_LABEL }
  await assertPortFreeBeforeMockMutation(
    managementPort,
    'management',
    ownershipOpts
  )
  await assertPortFreeBeforeMockMutation(servingPort, 'serving', ownershipOpts)
  return { managementPort, servingPort }
}
