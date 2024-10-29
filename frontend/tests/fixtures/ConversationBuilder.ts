import type { Conversation, Note } from "@/generated/backend"
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

  note(note: Note) {
    this.data.subject!.note = note
    return this
  }
  withoutId() {
    // biome-ignore lint/suspicious/noExplicitAny: <explanation>
    ;(this.data as any).id = undefined
    return this
  }

  do() {
    if (
      this.data.subject?.note === undefined &&
      this.data.subject?.assessmentQuestionInstance === undefined
    ) {
      this.data.subject!.note = new NoteBuilder().please()
    }
    return this.data
  }
}

export default ConversationBuilder
