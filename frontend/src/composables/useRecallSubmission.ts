import { onBeforeUnmount, ref, shallowRef, watch } from "vue"
import type { Ref } from "vue"
import type { AnsweredQuestion } from "@generated/donut-backend-api"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

export function useRecallSubmission<T extends object>(
  promptId: Ref<number | undefined>,
  request: (
    promptId: number,
    body: T
  ) => Promise<{ data?: AnsweredQuestion; error?: unknown }>,
  onAnswered: (answer: AnsweredQuestion) => void
) {
  const phase = ref<"ready" | "pending" | "uncertain" | "answered">("ready")
  const submission = shallowRef<T>()
  let mounted = true
  onBeforeUnmount(() => {
    mounted = false
  })
  watch(promptId, () => {
    submission.value = undefined
    phase.value = "ready"
  })

  const sendSubmission = async () => {
    const id = promptId.value
    phase.value = "pending"
    if (id === undefined) return
    const { data, error } = await apiCallWithLoading(() =>
      request(id, submission.value!)
    )
    if (!mounted || promptId.value !== id) return
    if (error) phase.value = "uncertain"
    else {
      phase.value = "answered"
      onAnswered(data!)
    }
  }

  const submitAnswer = async (body: T) => {
    if (phase.value !== "ready") return
    submission.value = Object.freeze({ ...body })
    await sendSubmission()
  }
  const retryAnswer = async () => {
    if (phase.value !== "uncertain") return
    await sendSubmission()
  }
  return { phase, submission, submitAnswer, retryAnswer }
}
