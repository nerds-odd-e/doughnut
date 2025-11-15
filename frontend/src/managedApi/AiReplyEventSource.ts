import createEventSourceWithBody from "./createEventSourceWithBody"

class AiReplyEventSource {
  private conversationId: number

  public onMessageCallback: (event: string, data: string) => void = () =>
    undefined

  public onErrorCallback?: (error: unknown) => void = undefined

  constructor(conversationId: number) {
    this.conversationId = conversationId
  }

  onMessage(callback: (event: string, data: string) => void): this {
    this.onMessageCallback = callback
    return this
  }

  onError(callback: (error: unknown) => void): this {
    this.onErrorCallback = callback
    return this
  }

  start(): void {
    const url = `/api/conversation/${this.conversationId}/ai-reply`
    createEventSourceWithBody(
      url,
      undefined,
      this.onMessageCallback,
      this.onErrorCallback
    )
  }
}

export default AiReplyEventSource
