<template>
  <ol class="choices grid grid-cols-1 sm:grid-cols-2 list-none p-0 gap-4 mt-4" v-if="choices && choices.length > 0">
    <li
      class="choice min-h-[80px]"
      v-for="(choice, index) in choices"
      :key="index"
    >
      <button
        :class="[
          'w-full h-full flex justify-center items-center',
          'rounded-lg bg-base-200 p-4',
          'choice-button',
          'focus:outline-none focus:ring-2 focus:ring-primary',
          'disabled:opacity-65 transition-colors',
          {
            'is-correct': isOptionCorrect(index),
            'is-incorrect': !isOptionCorrect(index),
            'is-selected': isSelectedOption(index),
          }
        ]"
        @click.once="submitAnswer({ choiceIndex: index })"
        :disabled="disabled"
      >
        <div
          v-html="getChoiceHtml(choice)"
          class="whitespace-normal break-words choice-text"
          @click.capture="handleInnerClick"
        />
      </button>
    </li>
  </ol>
</template>



<script lang="ts">
import type { AnswerDto } from "@generated/doughnut-backend-api"
import { defineComponent } from "vue"
import markdownizer from "../form/markdownizer"

export default defineComponent({
  props: {
    choices: {
      type: Array<string>,
    },
    correctChoiceIndex: Number,
    answerChoiceIndex: Number,
    disabled: Boolean,
  },
  emits: ["answer"],
  methods: {
    handleInnerClick(event: Event) {
      // Prevent any link clicks from navigating, but allow text selection
      if (event.target instanceof HTMLAnchorElement) {
        event.preventDefault()
        event.stopPropagation()
      }
    },
    isSelectedOption(optionIndex: number) {
      return this.answerChoiceIndex === optionIndex
    },
    isOptionCorrect(index: number) {
      return index === this.correctChoiceIndex
    },
    async submitAnswer(answerData: AnswerDto) {
      this.$emit("answer", answerData)
    },
    getChoiceHtml(choice: string) {
      return markdownizer.markdownToHtml(choice)
    },
  },
})
</script>

<style scoped lang="sass">
.choices
  list-style-type: none
  padding-left: 0

.choice
  min-height: 80px

.is-correct
  background-color: hsl(142, 76%, 36%) !important
  color: white !important
  border: 2px solid hsl(142, 76%, 28%) !important

.is-selected:not(.is-correct)
  font-weight: bold
  background-color: hsl(0, 84%, 60%) !important
  color: white !important
  border: 2px solid hsl(0, 84%, 50%) !important

button, a, input
  border: 0
  -webkit-tap-highlight-color: rgba(0,0,0,0)
  -webkit-touch-callout: none
  -webkit-user-select: none

.choice-text
  user-select: text
  -webkit-user-select: text
  -moz-user-select: text
  -ms-user-select: text

.choice-button
  @media (hover: hover)
    &:hover:not(:disabled)
      background-color: var(--color-base-300) !important
</style>
