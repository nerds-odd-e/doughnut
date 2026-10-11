import { watch } from "vue"
import type { DictationTarget } from "@/models/audio/dictationTarget"
import { useTitleVoiceInput } from "./useTitleVoiceInput"

/** One title recording and insertion session, shared by its controls. */
export function useTitleDictation(begin: () => DictationTarget) {
  let session: DictationTarget | undefined
  const voiceInput = useTitleVoiceInput((segments) => session!.insert(segments))
  const { busy } = voiceInput

  const end = (placeCaret: boolean) => {
    session?.end(placeCaret)
    session = undefined
  }

  watch(busy, (isBusy) => {
    if (isBusy) session = begin()
    else end(true)
  })

  return {
    busy,
    voiceInput,
    /** Ends the session without moving focus. */
    leave: () => {
      end(false)
      voiceInput.leave()
    },
  }
}
