import { ref, watch } from "vue"
import type { DictationTarget } from "@/models/audio/dictationTarget"

/** One dictation session for a title: it begins when the Speak the title control becomes busy and ends when the control is idle again, or when the author leaves the title. */
export function useTitleDictation(begin: () => DictationTarget) {
  const busy = ref(false)
  let session: DictationTarget | undefined

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
    insertHeardSegments: (segments: string[]) => session!.insert(segments),
    /** Ends the session without moving focus. */
    leave: () => {
      end(false)
      busy.value = false
    },
  }
}
