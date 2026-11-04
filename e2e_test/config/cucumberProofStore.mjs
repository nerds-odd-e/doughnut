// Successful scenario proof in this checkout: the Cucumber format of the
// shared proof store.
import { refreshProof, textOrder } from './proofStore.mjs'

const identityOf = ({ feature, scenario, example }) =>
  JSON.stringify([feature, scenario, example ?? null])

const cucumberFormat = {
  identityOf,
  compare: (left, right) => textOrder(identityOf(left), identityOf(right)),
}

export function maintainScenarioProof(directory, observation) {
  refreshProof(cucumberFormat, directory, observation)
}
