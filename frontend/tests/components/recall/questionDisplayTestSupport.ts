import type { Mcq } from "@generated/donut-backend-api"

export function questionDisplayProps(
  mcq: Pick<Mcq, "questionStem" | "responseChoices">
) {
  return {
    questionStem: mcq.questionStem,
    responseChoices: mcq.responseChoices,
  }
}
