export type RunStreamData = {
  runId: string
  fullMessage: string
  responseType?: 'requires action' | 'message delta'
}

export const createRequiresActionRun = (
  runId: string,
  functionName: string,
  argumentsObj: unknown
) => {
  return {
    id: runId,
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
  const { runId, fullMessage, responseType } = runStreamData
  if (responseType === 'requires action') {
    const actionData = createRequiresActionRun(
      runId,
      'complete_note_details',
      JSON.parse(fullMessage)
    )
    return `event: thread.run.requires_action
data: ${JSON.stringify(actionData)}
`
  }
  if (!responseType || responseType === 'message delta') {
    return `event: thread.message.delta
data: {"delta": {"content": [{"index": 0, "type": "text", "text": {"value": "${fullMessage}"}}]}}
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

`
}
