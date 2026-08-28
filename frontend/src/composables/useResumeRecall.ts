import { useRouter } from "vue-router"
import { primeSoftKeyboard } from "@/utils/focusTarget"
import { useRecallData } from "./useRecallData"

// Split out of useRecallData so the shared recall state stays router-free
// (it is pulled in by non-navigating consumers like useQuestionThinkingTime).
export function useResumeRecall() {
  const router = useRouter()
  const { toRepeat, currentIndex, treadmillMode, shouldResumeRecall } =
    useRecallData()

  const resumeRecall = () => {
    const current = toRepeat.value?.[currentIndex.value]
    if (current?.spelling && !treadmillMode.value) {
      primeSoftKeyboard()
    }
    shouldResumeRecall.value = true
    router.push({ name: "recall" })
  }

  return { resumeRecall }
}
