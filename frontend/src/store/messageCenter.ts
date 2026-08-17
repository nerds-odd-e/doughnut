import type { ConversationMessage } from "@generated/doughnut-backend-api"
import { reactive } from "vue"

export const messageCenter = reactive({
  unreadMessages: [] as ConversationMessage[],
})
