import { onUnmounted, watch, type InjectionKey } from "vue"
import type { DictationTarget } from "@/models/audio/dictationTarget"
import { useTitleVoiceInput } from "./useTitleVoiceInput"

/** One title recording and insertion session, shared by its controls. */
export function useTitleDictation(begin: () => DictationTarget) {
  let session: DictationTarget | undefined
  const voiceInput = useTitleVoiceInput(
    (segments) => session!.insert(segments),
    () => {
      session = begin()
    }
  )
  const { busy } = voiceInput

  const end = (placeCaret: boolean) => {
    session?.end(placeCaret)
    session = undefined
  }

  watch(busy, (isBusy) => {
    if (!isBusy) end(true)
  })

  const leave = () => {
    end(false)
    voiceInput.leave()
  }

  onUnmounted(leave)

  return {
    busy,
    voiceInput,
    /** Ends the session without moving focus. */
    leave,
  }
}

export type TitleDictation = ReturnType<typeof useTitleDictation>

export const noteTitleDictationKey: InjectionKey<{
  dictation: TitleDictation
  hasFocus: () => boolean
}> = Symbol("noteTitleDictation")
