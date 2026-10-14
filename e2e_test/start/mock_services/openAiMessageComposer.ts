function sseDataLine(obj: unknown): string {
  return `data: ${JSON.stringify(obj)}\n\n`
}

export function buildResponsesStreamEvent(
  message: string,
  responseType?: 'requires action' | 'message delta & complete'
): string {
  const chunks: string[] = []
  let seq = 0
  const nextSeq = () => ++seq
  const textItemId = 'msg-mock'

  if (responseType === 'requires action') {
    const toolCallData = JSON.parse(message) as Record<string, unknown>
    const argumentsString = JSON.stringify(toolCallData)
    const fcItemId = 'fc-mock'
    const callId = 'call-mock'

    chunks.push(
      sseDataLine({
        type: 'response.output_item.added',
        output_index: 0,
        sequence_number: nextSeq(),
        item: {
          type: 'function_call',
          id: fcItemId,
          call_id: callId,
          name: 'NoteContentCompletion',
          arguments: '',
        },
      })
    )

    const firstLen = Math.min(10, argumentsString.length)
    if (firstLen > 0) {
      chunks.push(
        sseDataLine({
          type: 'response.function_call_arguments.delta',
          item_id: fcItemId,
          output_index: 0,
          sequence_number: nextSeq(),
          delta: argumentsString.substring(0, firstLen),
        })
      )
    }
    let offset = firstLen
    while (offset < argumentsString.length) {
      const fragment = argumentsString.substring(
        offset,
        Math.min(offset + 20, argumentsString.length)
      )
      chunks.push(
        sseDataLine({
          type: 'response.function_call_arguments.delta',
          item_id: fcItemId,
          output_index: 0,
          sequence_number: nextSeq(),
          delta: fragment,
        })
      )
      offset += 20
    }

    chunks.push(
      sseDataLine({
        type: 'response.function_call_arguments.done',
        name: 'NoteContentCompletion',
        item_id: fcItemId,
        output_index: 0,
        sequence_number: nextSeq(),
        arguments: argumentsString,
      })
    )
  } else {
    chunks.push(
      sseDataLine({
        type: 'response.output_text.delta',
        delta: message,
        item_id: textItemId,
        output_index: 0,
        content_index: 0,
        sequence_number: nextSeq(),
        logprobs: [],
      })
    )
    chunks.push(
      sseDataLine({
        type: 'response.output_text.done',
        text: message,
        item_id: textItemId,
        output_index: 0,
        content_index: 0,
        sequence_number: nextSeq(),
        logprobs: [],
      })
    )
  }

  chunks.push('data: [DONE]\n\n')
  return chunks.join('')
}
