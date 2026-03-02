import type { Conversation, Note, RecallPrompt } from "@/generated/backend"
import Builder from "./Builder"
import generateId from "./generateId"
import NoteBuilder from "./NoteBuilder"

class ConversationBuilder extends Builder<Conversation> {
  data: Conversation = {
    id: generateId(),
    createdAt: "",
    updatedAt: "",
    subject: {},
  }

  withId(id: number) {
    this.data.id = id
    return this
  }

  forANote(note: Note) {
    this.data.subject!.note = note
    return this
  }
  forAnsweredQuestion(recallPrompt: RecallPrompt) {
    this.data.subject!.answeredQuestion = recallPrompt
    return this
  }
  withoutId() {
    // biome-ignore lint/suspicious/noExplicitAny: Needed to modify a property that might not exist in the type
    ;(this.data as any).id = undefined
    return this
  }

  do() {
    if (
      this.data.subject?.note === undefined &&
      this.data.subject?.assessmentQuestionInstance === undefined &&
      this.data.subject?.answeredQuestion === undefined
    ) {
      this.data.subject!.note = new NoteBuilder().please()
    }
    return this.data
  }
}

export default ConversationBuilder
