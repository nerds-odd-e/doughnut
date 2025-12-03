import { describe, it, expect, vi } from "vitest"
import { createAiReplyStates } from "@/models/aiReplyState"

describe("aiReplyState", () => {
  const mockContext = {
    set: vi.fn(),
    append: vi.fn(),
    reset: vi.fn(),
    handleSuggestion: vi.fn(),
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe("chat.completion.chunk", () => {
    it("appends content from message", async () => {
      const states = createAiReplyStates(mockContext)
      const chunk = {
        choices: [
          {
            index: 0,
            message: {
              role: "assistant",
              content: "Hello world",
            },
            finish_reason: null,
          },
        ],
      }

      await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk))

      expect(mockContext.append).toHaveBeenCalledWith("Hello world")
    })

    it("handles tool call successfully", async () => {
      mockContext.handleSuggestion.mockResolvedValue({ status: "accepted" })

      const states = createAiReplyStates(mockContext)

      // First chunk: tool call with id, name, and start of arguments
      const chunk1 = {
        choices: [
          {
            index: 0,
            delta: {
              role: "assistant",
              tool_calls: [
                {
                  index: 0,
                  id: "call-1",
                  type: "function",
                  function: {
                    name: "NoteDetailsCompletion",
                    arguments: '{"details":',
                  },
                },
              ],
            },
            finish_reason: null,
          },
        ],
      }

      await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk1))

      // Second chunk: continue arguments
      const chunk2 = {
        choices: [
          {
            index: 0,
            delta: {
              tool_calls: [
                {
                  index: 0,
                  function: {
                    arguments: '"test content"}',
                  },
                },
              ],
            },
            finish_reason: null,
          },
        ],
      }

      await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk2))

      // Final chunk: finish_reason triggers processing
      const chunk3 = {
        choices: [
          {
            index: 0,
            delta: {},
            finish_reason: "tool_calls",
          },
        ],
      }

      await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk3))

      expect(mockContext.handleSuggestion).toHaveBeenCalledWith({
        suggestionType: "completion",
        content: { details: "test content" },
        toolCallId: "call-1",
      })
      // Tool calls are executed inline with Chat Completion API
      // No need to submit results
    })

    it("handles title suggestion tool call", async () => {
      mockContext.handleSuggestion.mockResolvedValue({ status: "accepted" })

      const states = createAiReplyStates(mockContext)

      // First chunk: tool call with id, name, and arguments
      const chunk1 = {
        choices: [
          {
            index: 0,
            delta: {
              role: "assistant",
              tool_calls: [
                {
                  index: 0,
                  id: "call-1",
                  type: "function",
                  function: {
                    name: "TitleReplacement",
                    arguments: '{"newTitle":"New Title"}',
                  },
                },
              ],
            },
            finish_reason: null,
          },
        ],
      }

      await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk1))

      // Final chunk: finish_reason triggers processing
      const chunk2 = {
        choices: [
          {
            index: 0,
            delta: {},
            finish_reason: "tool_calls",
          },
        ],
      }

      await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk2))

      expect(mockContext.handleSuggestion).toHaveBeenCalledWith({
        suggestionType: "title",
        content: "New Title",
        toolCallId: "call-1",
      })
    })

    it("handles tool call rejection silently", async () => {
      mockContext.handleSuggestion.mockRejectedValue(
        new Error("Tool call was rejected")
      )

      const states = createAiReplyStates(mockContext)

      // First chunk: tool call
      const chunk1 = {
        choices: [
          {
            index: 0,
            delta: {
              tool_calls: [
                {
                  index: 0,
                  id: "call-1",
                  function: {
                    name: "NoteDetailsCompletion",
                    arguments: '{"details":"test"}',
                  },
                },
              ],
            },
            finish_reason: null,
          },
        ],
      }

      await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk1))

      // Final chunk: finish_reason triggers processing
      const chunk2 = {
        choices: [
          {
            index: 0,
            delta: {},
            finish_reason: "tool_calls",
          },
        ],
      }

      // Should not throw - rejection is handled silently
      await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk2))

      expect(mockContext.handleSuggestion).toHaveBeenCalled()
      // No API calls needed - rejection is handled inline
    })

    it("handles unknown tool call", async () => {
      mockContext.handleSuggestion.mockResolvedValue({ status: "skipped" })

      const states = createAiReplyStates(mockContext)

      // First chunk: tool call
      const chunk1 = {
        choices: [
          {
            index: 0,
            delta: {
              tool_calls: [
                {
                  index: 0,
                  id: "call-1",
                  function: {
                    name: "unknown_tool",
                    arguments: '{"test":"data"}',
                  },
                },
              ],
            },
            finish_reason: null,
          },
        ],
      }

      await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk1))

      // Final chunk: finish_reason triggers processing
      const chunk2 = {
        choices: [
          {
            index: 0,
            delta: {},
            finish_reason: "tool_calls",
          },
        ],
      }

      await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk2))

      expect(mockContext.handleSuggestion).toHaveBeenCalledWith({
        suggestionType: "unknown",
        content: {
          rawJson: '{"test":"data"}',
          functionName: "unknown_tool",
        },
        toolCallId: "call-1",
      })
    })

    it("accumulates tool calls from delta.tool_calls across multiple chunks", async () => {
      mockContext.handleSuggestion.mockResolvedValue({ status: "accepted" })

      const states = createAiReplyStates(mockContext)

      // First chunk: tool call starts with id and name
      const chunk1 = {
        choices: [
          {
            index: 0,
            delta: {
              tool_calls: [
                {
                  index: 0,
                  id: "call-123",
                  type: "function",
                  function: {
                    name: "NoteDetailsCompletion",
                    arguments: '{"details":',
                  },
                },
              ],
            },
            finish_reason: null,
          },
        ],
      }

      await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk1))

      // Second chunk: arguments continue (fragmented JSON)
      const chunk2 = {
        choices: [
          {
            index: 0,
            delta: {
              tool_calls: [
                {
                  index: 0,
                  id: null, // id can be null in subsequent chunks
                  function: {
                    arguments: '"test content"}',
                  },
                },
              ],
            },
            finish_reason: null,
          },
        ],
      }

      await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk2))

      // Final chunk: finish_reason triggers processing
      const chunk4 = {
        choices: [
          {
            index: 0,
            delta: {},
            finish_reason: "tool_calls",
          },
        ],
      }

      await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk4))

      // Verify the accumulated arguments were parsed correctly
      expect(mockContext.handleSuggestion).toHaveBeenCalledWith({
        suggestionType: "completion",
        content: { details: "test content" },
        toolCallId: "call-123",
      })
    })

    it("handles multiple tool calls with different indices", async () => {
      mockContext.handleSuggestion.mockResolvedValue({ status: "accepted" })

      const states = createAiReplyStates(mockContext)

      // First tool call (index 0)
      const chunk1 = {
        choices: [
          {
            index: 0,
            delta: {
              tool_calls: [
                {
                  index: 0,
                  id: "call-1",
                  function: {
                    name: "NoteDetailsCompletion",
                    arguments: '{"details":"first"}',
                  },
                },
              ],
            },
            finish_reason: null,
          },
        ],
      }

      await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk1))

      // Second tool call (index 1)
      const chunk2 = {
        choices: [
          {
            index: 0,
            delta: {
              tool_calls: [
                {
                  index: 1,
                  id: "call-2",
                  function: {
                    name: "TitleReplacement",
                    arguments: '{"newTitle":"Second Title"}',
                  },
                },
              ],
            },
            finish_reason: null,
          },
        ],
      }

      await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk2))

      // Final chunk triggers processing
      const chunk3 = {
        choices: [
          {
            index: 0,
            delta: {},
            finish_reason: "tool_calls",
          },
        ],
      }

      await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk3))

      // Both tool calls should be processed
      expect(mockContext.handleSuggestion).toHaveBeenCalledTimes(2)
      expect(mockContext.handleSuggestion).toHaveBeenCalledWith({
        suggestionType: "completion",
        content: { details: "first" },
        toolCallId: "call-1",
      })
      expect(mockContext.handleSuggestion).toHaveBeenCalledWith({
        suggestionType: "title",
        content: "Second Title",
        toolCallId: "call-2",
      })
    })

    it("handles fragmented arguments that need concatenation", async () => {
      mockContext.handleSuggestion.mockResolvedValue({ status: "accepted" })

      const states = createAiReplyStates(mockContext)

      // Simulate fragmented JSON arguments across multiple chunks
      const chunks = [
        {
          choices: [
            {
              index: 0,
              delta: {
                tool_calls: [
                  {
                    index: 0,
                    id: "call-frag",
                    function: {
                      name: "NoteDetailsCompletion",
                      arguments: '{"details":"This is a',
                    },
                  },
                ],
              },
              finish_reason: null,
            },
          ],
        },
        {
          choices: [
            {
              index: 0,
              delta: {
                tool_calls: [
                  {
                    index: 0,
                    function: {
                      arguments: "",
                    },
                  },
                ],
              },
              finish_reason: null,
            },
          ],
        },
        {
          choices: [
            {
              index: 0,
              delta: {
                tool_calls: [
                  {
                    index: 0,
                    function: {
                      arguments: " long",
                    },
                  },
                ],
              },
              finish_reason: null,
            },
          ],
        },
        {
          choices: [
            {
              index: 0,
              delta: {
                tool_calls: [
                  {
                    index: 0,
                    function: {
                      arguments: ' completion"}',
                    },
                  },
                ],
              },
              finish_reason: null,
            },
          ],
        },
        {
          choices: [
            {
              index: 0,
              delta: {},
              finish_reason: "tool_calls",
            },
          ],
        },
      ]

      for (const chunk of chunks) {
        await states["chat.completion.chunk"]?.handleEvent(
          JSON.stringify(chunk)
        )
      }

      // Verify the complete arguments were parsed correctly
      expect(mockContext.handleSuggestion).toHaveBeenCalledWith({
        suggestionType: "completion",
        content: {
          details: "This is a long completion",
        },
        toolCallId: "call-frag",
      })
    })

    it("clears accumulated tool calls after processing", async () => {
      mockContext.handleSuggestion.mockResolvedValue({ status: "accepted" })

      const states = createAiReplyStates(mockContext)

      // First tool call
      const chunk1 = {
        choices: [
          {
            index: 0,
            delta: {
              tool_calls: [
                {
                  index: 0,
                  id: "call-1",
                  function: {
                    name: "NoteDetailsCompletion",
                    arguments: '{"details":"first"}',
                  },
                },
              ],
            },
            finish_reason: "tool_calls",
          },
        ],
      }

      await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk1))
      expect(mockContext.handleSuggestion).toHaveBeenCalledTimes(1)

      // Second tool call in a new stream should not include the first one
      const chunk2 = {
        choices: [
          {
            index: 0,
            delta: {
              tool_calls: [
                {
                  index: 0,
                  id: "call-2",
                  function: {
                    name: "NoteDetailsCompletion",
                    arguments: '{"details":"second"}',
                  },
                },
              ],
            },
            finish_reason: "tool_calls",
          },
        ],
      }

      await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk2))

      // Should have been called twice (once for each tool call)
      expect(mockContext.handleSuggestion).toHaveBeenCalledTimes(2)
      expect(mockContext.handleSuggestion).toHaveBeenNthCalledWith(2, {
        suggestionType: "completion",
        content: { details: "second" },
        toolCallId: "call-2",
      })
    })
  })

  describe("done", () => {
    it("resets context", async () => {
      const states = createAiReplyStates(mockContext)
      await states.done?.handleEvent("")

      expect(mockContext.reset).toHaveBeenCalled()
    })
  })
})
