import { ClarifyingQuestion } from "@/generated/backend"

export default interface ClarifyingQuestionAndAnswer {
  questionFromAI: ClarifyingQuestion
  answerFromUser: string
}
