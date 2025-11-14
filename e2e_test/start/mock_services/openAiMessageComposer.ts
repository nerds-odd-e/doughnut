// Chat completion streaming event builder (native format)
export function buildChatCompletionStreamEvent(
  message: string,
  responseType?: 'requires action' | 'message delta & complete'
): string {
  if (responseType === 'requires action') {
    // Tool call response
    const toolCallData = JSON.parse(message)
    const chunk = {
      choices: [
        {
          index: 0,
          message: {
            role: 'assistant',
            content: null,
            tool_calls: [
              {
                id: 'tool-call-1',
                type: 'function',
                function: {
                  name: 'complete_note_details',
                  arguments: JSON.stringify(toolCallData),
                },
              },
            ],
          },
          finish_reason: 'tool_calls',
        },
      ],
    }
    return `data: ${JSON.stringify(chunk)}

data: [DONE]

`
  }

  // Regular message response - use delta for streaming (matches real API)
  // Send content as delta chunks
  const chunks: string[] = []
  // For simplicity, send as one delta chunk (in real streaming, this would be character by character)
  const deltaChunk = {
    choices: [
      {
        index: 0,
        delta: {
          role: 'assistant',
          content: message,
        },
        finish_reason: null,
      },
    ],
  }
  chunks.push(`data: ${JSON.stringify(deltaChunk)}`)

  // Final chunk with finish reason
  const finalChunk = {
    choices: [
      {
        index: 0,
        delta: {},
        finish_reason: 'stop',
      },
    ],
  }
  chunks.push(`data: ${JSON.stringify(finalChunk)}`)
  chunks.push('data: [DONE]')

  return `${chunks.join('\n\n')}\n\n`
}
