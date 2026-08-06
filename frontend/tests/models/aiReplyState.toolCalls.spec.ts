import { describe, it, expect, beforeEach } from "vitest"
import { createAiReplyStates } from "@/models/aiReplyState"
import {
  createMockAiReplyContext,
  handleChunks,
  type MockAiReplyContext,
  toolCallArgsChunk,
  toolCallFinishInOneChunk,
  toolCallStartChunk,
  toolCallsFinishChunk,
} from "./aiReplyStateTestSupport"

describe("aiReplyState tool calls", () => {
  let mockContext: MockAiReplyContext

  beforeEach(() => {
    mockContext = createMockAiReplyContext()
  })

  it("accumulates NoteContentCompletion arguments across chunks", async () => {
    mockContext.handleSuggestion.mockResolvedValue({ status: "accepted" })
    const states = createAiReplyStates(mockContext)

    await handleChunks(states, [
      toolCallStartChunk({
        id: "call-1",
        name: "NoteContentCompletion",
        arguments: '{"content":',
      }),
      toolCallArgsChunk('"test content"}', { id: null }),
      toolCallsFinishChunk(),
    ])

    expect(mockContext.handleSuggestion).toHaveBeenCalledWith({
      suggestionType: "completion",
      content: { content: "test content" },
      toolCallId: "call-1",
    })
  })

  it("handles TitleReplacement tool call", async () => {
    mockContext.handleSuggestion.mockResolvedValue({ status: "accepted" })
    const states = createAiReplyStates(mockContext)

    await handleChunks(states, [
      toolCallStartChunk({
        id: "call-1",
        name: "TitleReplacement",
        arguments: '{"newTitle":"New Title"}',
      }),
      toolCallsFinishChunk(),
    ])

    expect(mockContext.handleSuggestion).toHaveBeenCalledWith({
      suggestionType: "title",
      content: "New Title",
      toolCallId: "call-1",
    })
  })

  it("handles tool call rejection without throwing", async () => {
    mockContext.handleSuggestion.mockRejectedValue(
      new Error("Tool call was rejected")
    )
    const states = createAiReplyStates(mockContext)

    await handleChunks(states, [
      toolCallStartChunk({
        id: "call-1",
        name: "NoteContentCompletion",
        arguments: '{"content":"test"}',
      }),
      toolCallsFinishChunk(),
    ])

    expect(mockContext.handleSuggestion).toHaveBeenCalled()
  })

  it("forwards unknown tool calls as unknown suggestions", async () => {
    mockContext.handleSuggestion.mockResolvedValue({ status: "skipped" })
    const states = createAiReplyStates(mockContext)

    await handleChunks(states, [
      toolCallStartChunk({
        id: "call-1",
        name: "unknown_tool",
        arguments: '{"test":"data"}',
      }),
      toolCallsFinishChunk(),
    ])

    expect(mockContext.handleSuggestion).toHaveBeenCalledWith({
      suggestionType: "unknown",
      content: {
        rawJson: '{"test":"data"}',
        functionName: "unknown_tool",
      },
      toolCallId: "call-1",
    })
  })

  it("processes multiple tool calls with different indices", async () => {
    mockContext.handleSuggestion.mockResolvedValue({ status: "accepted" })
    const states = createAiReplyStates(mockContext)

    await handleChunks(states, [
      toolCallStartChunk({
        id: "call-1",
        name: "NoteContentCompletion",
        arguments: '{"content":"first"}',
        index: 0,
      }),
      toolCallStartChunk({
        id: "call-2",
        name: "TitleReplacement",
        arguments: '{"newTitle":"Second Title"}',
        index: 1,
      }),
      toolCallsFinishChunk(),
    ])

    expect(mockContext.handleSuggestion).toHaveBeenCalledTimes(2)
    expect(mockContext.handleSuggestion).toHaveBeenCalledWith({
      suggestionType: "completion",
      content: { content: "first" },
      toolCallId: "call-1",
    })
    expect(mockContext.handleSuggestion).toHaveBeenCalledWith({
      suggestionType: "title",
      content: "Second Title",
      toolCallId: "call-2",
    })
  })

  it("concatenates fragmented arguments including empty middle chunks", async () => {
    mockContext.handleSuggestion.mockResolvedValue({ status: "accepted" })
    const states = createAiReplyStates(mockContext)

    await handleChunks(states, [
      toolCallStartChunk({
        id: "call-frag",
        name: "NoteContentCompletion",
        arguments: '{"content":"This is a',
      }),
      toolCallArgsChunk(""),
      toolCallArgsChunk(" long"),
      toolCallArgsChunk(' completion"}'),
      toolCallsFinishChunk(),
    ])

    expect(mockContext.handleSuggestion).toHaveBeenCalledWith({
      suggestionType: "completion",
      content: { content: "This is a long completion" },
      toolCallId: "call-frag",
    })
  })

  it("clears accumulated tool calls after processing", async () => {
    mockContext.handleSuggestion.mockResolvedValue({ status: "accepted" })
    const states = createAiReplyStates(mockContext)

    await handleChunks(states, [
      toolCallFinishInOneChunk({
        id: "call-1",
        name: "NoteContentCompletion",
        arguments: '{"content":"first"}',
      }),
    ])
    expect(mockContext.handleSuggestion).toHaveBeenCalledTimes(1)

    await handleChunks(states, [
      toolCallFinishInOneChunk({
        id: "call-2",
        name: "NoteContentCompletion",
        arguments: '{"content":"second"}',
      }),
    ])

    expect(mockContext.handleSuggestion).toHaveBeenNthCalledWith(2, {
      suggestionType: "completion",
      content: { content: "second" },
      toolCallId: "call-2",
    })
  })
})
