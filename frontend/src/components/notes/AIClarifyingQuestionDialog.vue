<template>
  <div>
    <ul>
      <li
        v-for="({ questionFromAI, answerFromUser }, index) in clarifyingHistory"
        :key="index"
      >
        {{ questionFromAI }}
        <ul>
          <li>{{ answerFromUser }}</li>
        </ul>
      </li>
    </ul>
  </div>
  <form @submit.prevent="handleFormSubmit">
    <h3>
      Clarification question by the AI:
      <strong>{{ clarifyingQuestion.question }}</strong>
    </h3>
    <TextInput
      scope-name="note"
      field="answerToAI"
      v-model="answerToAI"
      v-focus
    />
    <input type="submit" value="Send" class="btn btn-primary" />
  </form>
</template>

<script lang="ts">
import type { ClarifyingQuestion } from "@/generated/backend"
import type ClarifyingQuestionAndAnswer from "@/models/ClarifyingQuestionAndAnswer"
import type { PropType } from "vue"
import { defineComponent } from "vue"
import TextInput from "../form/TextInput.vue"

export default defineComponent({
  props: {
    clarifyingQuestion: {
      type: Object as PropType<ClarifyingQuestion>,
      required: true,
    },
    clarifyingHistory: {
      type: Array as PropType<ClarifyingQuestionAndAnswer[]>,
      required: true,
    },
  },
  components: { TextInput },
  emits: ["submit"],
  data() {
    return {
      answerToAI: "",
    }
  },
  methods: {
    handleFormSubmit() {
      this.$emit("submit", <ClarifyingQuestionAndAnswer>{
        questionFromAI: this.clarifyingQuestion,
        answerFromUser: this.answerToAI,
      })
    },
  },
})
</script>

<style lang="scss" scoped></style>
