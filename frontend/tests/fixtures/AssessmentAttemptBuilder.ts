import { AssessmentAttempt, QuizQuestion } from "@/generated/backend"
import Builder from "./Builder"
import generateId from "./generateId"

class AssessmentAttemptBuilder extends Builder<AssessmentAttempt> {
  private data: Partial<AssessmentAttempt> = {}

  withQuestions(questions: QuizQuestion[]) {
    this.data.quizQuestions = questions
    return this
  }
  passed(): AssessmentAttemptBuilder {
    this.data.isPass = true
    return this
  }

  do(): AssessmentAttempt {
    const id = generateId()
    return {
      ...this.data,
      id,
      submittedAt: "2021-09-01T00:00:00Z",
      notebookTitle: `Notebook ${id}`,
    }
  }
}

export default AssessmentAttemptBuilder
