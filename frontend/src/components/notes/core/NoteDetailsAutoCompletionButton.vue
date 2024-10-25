<template>
  <a
    :title="'auto-complete details'"
    class="btn btn-sm"
    role="button"
    @click="initialAutoCompleteDetails"
  >
    <SvgRobot />
    <Modal
      v-if="clarifyingQuestion"
      @close_request="clarifyingQuestion = undefined"
    >
      <template #body>
        <AIClarifyingQuestionDialog
          :clarifying-question="clarifyingQuestion"
          :clarifying-history="clarifyingHistory"
          @submit="clarifyingQuestionAnswered"
        />
      </template>
    </Modal>
  </a>
</template>

<script lang="ts">
import type {
  AiAssistantResponse,
  ClarifyingQuestion,
  Note,
} from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type ClarifyingQuestionAndAnswer from "@/models/ClarifyingQuestionAndAnswer"
import type { StorageAccessor } from "@/store/createNoteStorage"
import type { PropType } from "vue"
import { defineComponent } from "vue"
import Modal from "../../commons/Modal.vue"
import SvgRobot from "../../svgs/SvgRobot.vue"
import AIClarifyingQuestionDialog from "../AIClarifyingQuestionDialog.vue"

export default defineComponent({
  setup() {
    return {
      ...useLoadingApi(),
    }
  },
  props: {
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    note: {
      type: Object as PropType<Note>,
      required: true,
    },
  },
  components: {
    SvgRobot,
    Modal,
    AIClarifyingQuestionDialog,
  },
  data() {
    return {
      isUnmounted: false,
      threadRespons: undefined as undefined | AiAssistantResponse,
      clarifyingQuestion: undefined as undefined | ClarifyingQuestion,
      clarifyingHistory: [] as ClarifyingQuestionAndAnswer[],
    }
  },
  methods: {
    async initialAutoCompleteDetails() {
      const response = await this.managedApi.restAiController.getCompletion(
        this.note.id,
        {
          detailsToComplete: this.note.details,
        }
      )

      return this.autoCompleteDetails(response)
    },
    async autoCompleteDetails(response: AiAssistantResponse) {
      if (this.isUnmounted) return

      if (response.requiredAction?.clarifyingQuestion) {
        this.threadRespons = response
        this.clarifyingQuestion = response.requiredAction.clarifyingQuestion
        return
      }

      this.clarifyingQuestion = undefined
      this.storageAccessor
        .storedApi()
        .appendDetails(this.note.id, response.requiredAction!.contentToAppend!)
    },
    async clarifyingQuestionAnswered(
      clarifyingQuestionAndAnswer: ClarifyingQuestionAndAnswer
    ) {
      this.clarifyingHistory.push(clarifyingQuestionAndAnswer)
      const response =
        await this.managedApi.restAiController.answerCompletionClarifyingQuestion(
          {
            detailsToComplete: this.note.details,
            answer: clarifyingQuestionAndAnswer.answerFromUser,
            threadId: this.threadRespons!.threadId,
            runId: this.threadRespons!.runId,
            toolCallId: this.threadRespons!.requiredAction!.toolCallId,
          }
        )
      await this.autoCompleteDetails(response)
    },
  },
  unmounted() {
    this.isUnmounted = true
  },
})
</script>
