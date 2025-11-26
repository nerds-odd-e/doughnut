<template>
  <ol class="choices daisy-flex daisy-flex-wrap daisy-flex-row daisy-justify-start daisy-list-none daisy-p-0" v-if="choices && choices.length > 0">
    <li
      class="choice daisy-w-[46%] daisy-min-h-[80px] daisy-m-[2%] sm:daisy-w-full"
      v-for="(choice, index) in choices"
      :key="index"
    >
      <button
        :class="[
          'daisy-w-full daisy-h-full daisy-flex daisy-justify-center daisy-items-center',
          'daisy-rounded-lg daisy-bg-base-200',
          'hover:daisy-bg-primary hover:daisy-text-primary-content',
          'focus:daisy-outline-none focus:daisy-ring-2 focus:daisy-ring-primary',
          'disabled:daisy-opacity-65 daisy-transition-colors daisy-select-none',
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
          class="daisy-whitespace-normal daisy-break-words"
          @click.capture.prevent="handleInnerClick"
        />
      </button>
    </li>
  </ol>
</template>



<script lang="ts">
import type { AnswerDto } from "@generated/backend"
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
      // Prevent any link clicks from navigating
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
  display: flex
  flex-wrap: wrap
  flex-direction: row
  justify-content: flex-start
  list-style-type: none
  padding-left: 0

.choice
  width: 46%
  min-height: 80px
  margin: 2%
  @media(max-width: 500px)
    width: 100%

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
</style>
