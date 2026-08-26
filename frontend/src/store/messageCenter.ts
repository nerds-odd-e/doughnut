import type { ConversationMessage } from "@generated/donut-backend-api"
import { reactive } from "vue"

export const messageCenter = reactive({
  unreadMessages: [] as ConversationMessage[],
})
