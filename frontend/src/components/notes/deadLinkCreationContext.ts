import type { InjectionKey } from "vue"

export const openDeadLinkCreationKey: InjectionKey<(title: string) => void> =
  Symbol("openDeadLinkCreation")
