export type DailyProbeSide = "left" | "right"

export interface DailyProbeTrial {
  stimulus: DailyProbeSide
  response?: DailyProbeSide
  rtMs?: number
  correct: boolean
}

export const DAILY_PROBE_TIMEOUT_MS = 2000
export const DAILY_PROBE_ISI_MS = 2000
export const DAILY_PROBE_INSTRUCTION =
  "Each trial shows ← or →. Tap the left or right side, or press F for left, J for right (arrow keys also work). Go as fast as you can without mistakes."
const FALSE_START_MS = 100
const LAPSE_MS = 500

export const dailyProbePracticeSequence = [
  "left",
  "right",
  "right",
  "left",
] as const satisfies readonly DailyProbeSide[]

export const dailyProbeScoredSequence = [
  "left",
  "right",
  "left",
  "left",
  "right",
  "right",
  "left",
  "right",
  "left",
  "right",
  "right",
  "left",
  "left",
  "right",
  "left",
  "right",
  "right",
  "left",
  "right",
  "left",
] as const satisfies readonly DailyProbeSide[]

export const dailyProbeRunSequence = [
  ...dailyProbePracticeSequence,
  ...dailyProbeScoredSequence,
] as const satisfies readonly DailyProbeSide[]

export function mapDailyProbeKey(key: string): DailyProbeSide | undefined {
  if (key === "f" || key === "F" || key === "ArrowLeft") return "left"
  if (key === "j" || key === "J" || key === "ArrowRight") return "right"
  return undefined
}

export function recordDailyProbeTrial(input: {
  stimulus: DailyProbeSide
  stimulusOnsetMs: number
  responseMs?: number
  response?: DailyProbeSide
}): DailyProbeTrial {
  const { stimulus, stimulusOnsetMs, responseMs, response } = input
  if (response === undefined || responseMs === undefined) {
    return { stimulus, correct: false }
  }

  const rtMs = responseMs - stimulusOnsetMs
  if (rtMs >= DAILY_PROBE_TIMEOUT_MS) {
    return { stimulus, correct: false }
  }
  if (rtMs < FALSE_START_MS) {
    return { stimulus, response, correct: false }
  }

  return {
    stimulus,
    response,
    rtMs,
    correct: response === stimulus,
  }
}

function correctValidReciprocals(trials: readonly DailyProbeTrial[]): number[] {
  return trials.flatMap((trial) =>
    trial.correct && trial.rtMs !== undefined ? [1 / (trial.rtMs / 1000)] : []
  )
}

function mean(values: readonly number[]): number {
  return values.reduce((sum, value) => sum + value, 0) / values.length
}

export function dailyProbeSpeed(
  trials: readonly DailyProbeTrial[]
): number | undefined {
  const reciprocals = correctValidReciprocals(trials)
  if (reciprocals.length === 0) return
  return mean(reciprocals)
}

export function dailyProbeAccuracy(trials: readonly DailyProbeTrial[]): number {
  const correctCount = trials.filter((trial) => trial.correct).length
  return Math.round((100 * correctCount) / dailyProbeScoredSequence.length)
}

export function dailyProbeLapseCount(
  trials: readonly DailyProbeTrial[]
): number {
  return trials.filter((trial) =>
    trial.rtMs !== undefined
      ? trial.rtMs >= LAPSE_MS
      : trial.response === undefined
  ).length
}

export function dailyProbeVariability(
  trials: readonly DailyProbeTrial[]
): number | undefined {
  const reciprocals = correctValidReciprocals(trials)
  if (reciprocals.length < 2) return
  const meanReciprocal = mean(reciprocals)
  const sumSquaredDiffs = reciprocals.reduce(
    (sum, reciprocal) => sum + (reciprocal - meanReciprocal) ** 2,
    0
  )
  return Math.sqrt(sumSquaredDiffs / (reciprocals.length - 1))
}
