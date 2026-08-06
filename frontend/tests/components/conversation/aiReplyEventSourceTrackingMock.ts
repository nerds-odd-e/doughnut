import { vi } from "vitest"

/** Shared SSE instance-tracking mock for AiResponse specs (true external). */
export async function aiReplyEventSourceTrackingMock() {
  const actual = await vi.importActual<
    typeof import("@/managedApi/AiReplyEventSource")
  >("@/managedApi/AiReplyEventSource")
  const { setLastInstance } = await import(
    "@tests/helpers/aiReplyEventSourceTracker"
  )
  return {
    default: class extends actual.default {
      constructor(conversationId: number) {
        super(conversationId)
        setLastInstance(this)
      }
    },
  }
}
