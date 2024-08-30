<template>
  <ol class="choices" v-if="choices && choices.length > 0">
    <li class="choice" v-for="(choice, index) in choices" :key="index">
      <button
        :class="{
          'is-correct': isOptionCorrect(index),
          'current-choice': assessmentCurrentChoiceIndex === index,
          'is-selected': isSelectedOption(index),
        }"
        @click.once="submitAnswer({ choiceIndex: index })"
        :disabled="disabled"
      >
        <div v-html="choice" />
      </button>
    </li>
  </ol>
</template>

<style scoped lang="sass">
.choices
  display: flex
  flex-wrap: wrap
  flex-direction: row
  justify-content: flex-start
.choice
  width: 46%
  min-height: 80px
  margin: 2%
  @media(max-width: 500px)
    width: 100%
  button
    width: 100%
    height: 100%
    display: flex
    justify-content: center
    align-items: center
    border: 0
    border-radius: 0.5rem
    background-color: #e8e9ea
    color: #212529
    text-decoration: none
    white-space: normal
    word-break: break-word
    cursor: pointer
    transition: color 0.15s ease-in-out, background-color 0.15s ease-in-out, border-color 0.15s ease-in-out, box-shadow 0.15s ease-in-out
    &:hover
      color: #fff
      background-color: #007bff
      border-color: #007bff
    &:focus
      outline: 0
      box-shadow: 0 0 0 0.2rem rgba(0, 123, 255, 0.25)
    &:disabled
      opacity: 0.65

.is-correct
  background-color: #00ff00 !important

.current-choice
  border: solid 4px red !important

.is-selected
  font-weight: bold
  border-color: #000000
  border: 2
</style>

<script lang="ts">
import { AnswerDTO } from "@/generated/backend"
import { defineComponent } from "vue"

export default defineComponent({
  props: {
    choices: {
      type: Array<string>,
    },
    correctChoiceIndex: Number,
    answerChoiceIndex: Number,
    disabled: Boolean,
    assessmentCurrentChoiceIndex: Number,
    answeredCurrentQuestion: Boolean,
  },
  emits: ["answer"],
  data() {
    return {
      answer: "" as string,
    }
  },
  methods: {
    isSelectedOption(optionIndex: number) {
      return this.answerChoiceIndex === optionIndex
    },
    isOptionCorrect(index: number) {
      return index === this.correctChoiceIndex
    },
    async submitAnswer(answerData: AnswerDTO) {
      this.$emit("answer", answerData)
    },
  },
})
</script>

<style scoped>
.choices {
    list-style-type: none;
    padding-left: 0
}
</style>
