export type RunStreamData = {
  runId: string
  fullMessage: string
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

function buildEvent(runStreamData: RunStreamData): string {
  const { fullMessage } = runStreamData
  return `event: thread.message.delta
data: {"delta": {"content": [{"index": 0, "type": "text", "text": {"value": "${fullMessage}"}}]}}
`
}

export function buildRunStreamEvent(
  threadId: string,
  runStreamData: RunStreamData
): string {
  const { runId } = runStreamData
  return `event: thread.message.created
data: {"thread_id": "${threadId}", "run_id": "${runId}", "role": "assistant", "content": []}

${buildEvent(runStreamData)}
event: thread.run.step.completed
data: {"run_id": "${runId}", "status": "completed"}

`
}
