import { describe, it, expect, beforeEach } from "vitest"
import { createAiReplyStates } from "@/models/aiReplyState"
import {
  contentChunk,
  createMockAiReplyContext,
  type MockAiReplyContext,
} from "./aiReplyStateTestSupport"

describe("aiReplyState content", () => {
  let mockContext: MockAiReplyContext

  beforeEach(() => {
    mockContext = createMockAiReplyContext()
  })

  it("appends content from message", async () => {
    const states = createAiReplyStates(mockContext)

    await states["chat.completion.chunk"]?.handleEvent(
      JSON.stringify(contentChunk("Hello world"))
    )

    expect(mockContext.append).toHaveBeenCalledWith("Hello world")
  })

  it("resets context on done", async () => {
    const states = createAiReplyStates(mockContext)
    await states.done?.handleEvent("")

    expect(mockContext.reset).toHaveBeenCalled()
  })
})
