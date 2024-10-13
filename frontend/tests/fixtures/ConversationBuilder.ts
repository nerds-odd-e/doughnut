import type { Conversation } from "@/generated/backend"
import Builder from "./Builder"
import generateId from "./generateId"

class ConversationBuilder extends Builder<Conversation> {
  data: Conversation = {
    id: generateId(),
    createdAt: "",
    updatedAt: "",
  }

  do() {
    return this.data
  }
}

export default ConversationBuilder
