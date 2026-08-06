import { vi } from "vitest"

export function createMockAiReplyContext() {
  return {
    set: vi.fn(),
    append: vi.fn(),
    reset: vi.fn(),
    handleSuggestion: vi.fn(),
  }
}

export type MockAiReplyContext = ReturnType<typeof createMockAiReplyContext>

export async function handleChunks(
  states: {
    "chat.completion.chunk"?: { handleEvent: (data: string) => Promise<void> }
  },
  chunks: unknown[]
) {
  for (const chunk of chunks) {
    await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk))
  }
}

export function contentChunk(content: string) {
  return {
    choices: [
      {
        index: 0,
        message: { role: "assistant", content },
        finish_reason: null,
      },
    ],
  }
}

export function toolCallStartChunk(options: {
  id: string
  name: string
  arguments: string
  index?: number
}) {
  const index = options.index ?? 0
  return {
    choices: [
      {
        index: 0,
        delta: {
          role: "assistant",
          tool_calls: [
            {
              index,
              id: options.id,
              type: "function",
              function: {
                name: options.name,
                arguments: options.arguments,
              },
            },
          ],
        },
        finish_reason: null,
      },
    ],
  }
}

export function toolCallArgsChunk(
  argumentsFragment: string,
  options?: { index?: number; id?: string | null }
) {
  const index = options?.index ?? 0
  return {
    choices: [
      {
        index: 0,
        delta: {
          tool_calls: [
            {
              index,
              ...(options?.id !== undefined ? { id: options.id } : {}),
              function: { arguments: argumentsFragment },
            },
          ],
        },
        finish_reason: null,
      },
    ],
  }
}

export function toolCallsFinishChunk() {
  return {
    choices: [
      {
        index: 0,
        delta: {},
        finish_reason: "tool_calls",
      },
    ],
  }
}

export function toolCallFinishInOneChunk(options: {
  id: string
  name: string
  arguments: string
}) {
  return {
    choices: [
      {
        index: 0,
        delta: {
          tool_calls: [
            {
              index: 0,
              id: options.id,
              function: {
                name: options.name,
                arguments: options.arguments,
              },
            },
          ],
        },
        finish_reason: "tool_calls",
      },
    ],
  }
}
