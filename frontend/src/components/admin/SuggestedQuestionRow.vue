<template>
  <tr>
    <td>
      {{ suggestedQuestion.preservedQuestion.multipleChoicesQuestion.f0__stem }}
    </td>
    <td>{{ suggestedQuestion.positiveFeedback ? "Positive" : "Negative" }}</td>
    <td>{{ suggestedQuestion.comment }}</td>
    <td>
      <div class="daisy-btn-group" role="group">
        <button
          v-if="!suggestedQuestion.positiveFeedback"
          class="daisy-btn daisy-btn-sm"
          @click="duplicateQuestion(suggestedQuestion)"
        >
          Duplicate
        </button>
        <button class="daisy-btn daisy-btn-sm" @click="chatStarter">Chat</button>
        <button
          class="daisy-btn daisy-btn-sm"
          @click="deleteSuggestedQuestion(suggestedQuestion)"
        >
          Del
        </button>
      </div>
    </td>
  </tr>
</template>

<script lang="ts">
import usePopups from "@/components/commons/Popups/usePopups"
import type { SuggestedQuestionForFineTuning } from "@generated/backend"
import { duplicate, delete_ } from "@generated/backend/sdk.gen"
import type { PropType } from "vue"

export default {
  setup() {
    return { ...usePopups() }
  },
  props: {
    suggestedQuestion: {
      type: Object as PropType<SuggestedQuestionForFineTuning>,
      required: true,
    },
  },
  emits: ["duplicated"],
  computed: {
    chatStarterMessage() {
      return `In the personal knowledge management system I am using, my notes represents atomic knowledge point and are organized in hirachical structure.
      The system helps me to remember and understand my notes better by asking me questions without revealing the note to me.
      I'm expect the question to be:
      1. A multiple-choice question based on the note in the current contextual path with only 1 correct answer.
      2. I should be able to answer the question without looking at the note.
      3. The question should be only about the note.
      Please assume the role of a Memory Assistant, and help me with the follow note content and the question generated for it.

      """
      ${this.suggestedQuestion.preservedNoteContent}
      """

      The question I got from the system was:
      ${JSON.stringify(this.suggestedQuestion.preservedQuestion)}

      `
    },
  },
  methods: {
    async duplicateQuestion(suggested: SuggestedQuestionForFineTuning) {
      const { data: duplicated, error } = await duplicate({
        path: { suggestedQuestion: suggested.id },
      })
      if (!error && duplicated) {
        this.$emit("duplicated", duplicated)
      }
    },
    chatStarter() {
      this.popups.alert(this.chatStarterMessage)
    },
    async deleteSuggestedQuestion(suggested: SuggestedQuestionForFineTuning) {
      if (
        await this.popups.confirm(
          `Are you sure to delete this suggestion (${suggested.preservedQuestion.multipleChoicesQuestion.f0__stem})?`
        )
      ) {
        await delete_({
          path: { suggestedQuestion: suggested.id },
        })
      }
    },
  },
}
</script>
