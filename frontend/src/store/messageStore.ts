import type { ConversationMessage } from "@generated/backend"
import { reactive } from "vue"

export const messageCenterConversations = reactive({
  unreadConversations: [] as ConversationMessage[],
})
