import type { ConversationMessage } from "@generated/doughnut-backend-api"
import { reactive } from "vue"

export const messageCenterConversations = reactive({
  unreadConversations: [] as ConversationMessage[],
})
