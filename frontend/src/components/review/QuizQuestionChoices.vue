<template>
  <ol class="choices" v-if="choices.length > 0" type="A">
    <li class="choice" v-for="(choice, index) in choices" :key="index">
      <button
        class="btn btn-secondary btn-lg"
        :class="{
          'is-correct': isOptionCorrect(index),
          'is-incorrect': !isOptionCorrect(index),
          'is-selected': isSelectedOption(index),
        }"
        @click.once="submitAnswer({ choiceIndex: index })"
        :disabled="disabled"
      >
        <div v-if="!choice.picture" v-html="choice.display" />
        <div v-else>
          <ShowPicture v-bind="choice.pictureWithMask" :opacity="1" />
        </div>
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
  height: 100%
.choice
  width: 46%
  margin: 2%
  @media(max-width: 500px)
    width: 100%
  button
    width: 100%
    height: 100%
    padding: 0
    display: flex
    justify-content: center
    align-items: center
    text-align: center
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

.is-selected
  font-weight: bold
  border-color: #000000
  border: 2
</style>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ShowPicture from "../notes/ShowPicture.vue";

export default defineComponent({
  props: {
    choices: {
      type: Object as PropType<Generated.Option[]>,
      required: true,
    },
    correctChoiceIndex: Number,
    answerChoiceIndex: Number,
    disabled: Boolean,
  },
  components: {
    ShowPicture,
  },
  emits: ["answer"],
  data() {
    return {
      answer: "" as string,
    };
  },
  methods: {
    isSelectedOption(optionIndex: number) {
      return this.answerChoiceIndex === optionIndex;
    },
    isOptionCorrect(index: number) {
      return index === this.correctChoiceIndex;
    },
    async submitAnswer(answerData: Partial<Generated.Answer>) {
      this.$emit("answer", answerData);
    },
  },
});
</script>
