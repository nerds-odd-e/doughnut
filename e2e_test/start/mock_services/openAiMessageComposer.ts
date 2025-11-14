// Chat completion streaming event builder (native format)
export function buildChatCompletionStreamEvent(
  message: string,
  responseType?: 'requires action' | 'message delta & complete'
): string {
  if (responseType === 'requires action') {
    // Tool call response - realistic streaming format with delta.tool_calls
    const toolCallData = JSON.parse(message)
    const argumentsString = JSON.stringify(toolCallData)

    // Split arguments into fragments to simulate realistic streaming
    // First chunk: tool call id, type, and function name, plus start of arguments
    const firstChunk = {
      choices: [
        {
          index: 0,
          delta: {
            role: 'assistant',
            tool_calls: [
              {
                index: 0,
                id: 'tool-call-1',
                type: 'function',
                function: {
                  name: 'complete_note_details',
                  arguments: argumentsString.substring(
                    0,
                    Math.min(10, argumentsString.length)
                  ),
                },
              },
            ],
          },
          finish_reason: null,
        },
      ],
    }

    const chunks: string[] = []
    chunks.push(`data: ${JSON.stringify(firstChunk)}`)

    // Subsequent chunks: continue with fragmented arguments
    if (argumentsString.length > 10) {
      let offset = 10
      while (offset < argumentsString.length) {
        const fragment = argumentsString.substring(
          offset,
          Math.min(offset + 20, argumentsString.length)
        )
        const deltaChunk = {
          choices: [
            {
              index: 0,
              delta: {
                tool_calls: [
                  {
                    index: 0,
                    id: null, // id can be null in subsequent chunks
                    function: {
                      arguments: fragment,
                    },
                  },
                ],
              },
              finish_reason: null,
            },
          ],
        }
        chunks.push(`data: ${JSON.stringify(deltaChunk)}`)
        offset += 20
      }
    }

    // Final chunk: finish_reason triggers processing
    const finalChunk = {
      choices: [
        {
          index: 0,
          delta: {},
          finish_reason: 'tool_calls',
        },
      ],
    }
    chunks.push(`data: ${JSON.stringify(finalChunk)}`)
    chunks.push('data: [DONE]')

    return `${chunks.join('\n\n')}\n\n`
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
