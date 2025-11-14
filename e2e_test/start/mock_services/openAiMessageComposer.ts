export type RunStreamData = {
  runId: string
  threadId: string
  fullMessage: string
  responseType?: 'requires action' | 'message delta & complete'
}

export const createRequiresActionRun = (
  runId: string,
  threadId: string,
  functionName: string,
  argumentsObj: unknown
) => {
  return {
    id: runId,
    thread_id: threadId,
    status: 'requires_action',
    required_action: {
      type: 'submit_tool_outputs',
      submit_tool_outputs: {
        tool_calls: [
          {
            type: 'function',
            function: {
              name: functionName,
              arguments: JSON.stringify(argumentsObj),
            },
          },
        ],
      },
    },
  }
}

function buildSSEEvent(runStreamData: RunStreamData): string {
  const { runId, threadId, fullMessage, responseType } = runStreamData
  if (responseType === 'requires action') {
    const actionData = createRequiresActionRun(
      runId,
      threadId,
      'complete_note_details',
      JSON.parse(fullMessage)
    )
    return `event: thread.run.requires_action
data: ${JSON.stringify(actionData)}
`
  }
  if (!responseType || responseType === 'message delta & complete') {
    return `event: thread.message.delta
data: {"delta": {"content": [{"index": 0, "type": "text", "text": {"value": "${fullMessage}"}}]}}

event: thread.message.completed
data: {"role":"assistant","content":[{"type":"text","text":{"value":"${fullMessage}"}}]}

`
  }

  throw new Error('Unknown response type')
}

export function buildRunStreamEvent(
  threadId: string,
  runStreamData: RunStreamData
): string {
  const { runId } = runStreamData
  return `event: thread.message.created
data: {"thread_id": "${threadId}", "run_id": "${runId}", "role": "assistant", "content": []}

${buildSSEEvent(runStreamData)}
event: thread.run.step.completed
data: {"run_id": "${runId}", "status": "completed"}

event: done
data: [DONE]

`
}

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
