import type AiReplyEventSource from "@/managedApi/AiReplyEventSource"

let lastInstance: InstanceType<typeof AiReplyEventSource> | null = null

export function setLastInstance(
  instance: InstanceType<typeof AiReplyEventSource> | null
) {
  lastInstance = instance
}

export function getLastInstance(): InstanceType<
  typeof AiReplyEventSource
> | null {
  return lastInstance
}

export function resetInstance() {
  lastInstance = null
}
