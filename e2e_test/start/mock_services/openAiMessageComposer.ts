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

// Chat completion streaming event builder
export function buildChatCompletionStreamEvent(message: string): string {
  // Send message as single chunk
  const chunk = {
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

  const doneChunk = {
    choices: [
      {
        index: 0,
        delta: {},
        finish_reason: 'stop',
      },
    ],
  }

  return `data: ${JSON.stringify(chunk)}

data: ${JSON.stringify(doneChunk)}

data: [DONE]

`
}
