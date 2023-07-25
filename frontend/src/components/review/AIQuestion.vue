<template>
  <p v-if="background">
    {{ background }}
  </p>
  <h3>
    {{ questionDescription }}
  </h3>
  <ol v-if="choices" type="A">
    <li
      v-for="(option, index) in choices"
      :key="index"
      :class="{
        'is-correct': isOptionCorrect(index),
        'is-incorrect': !isOptionCorrect(index),
        'is-selected': isSelectedOption(index),
      }"
    >
      <button @click="selectOption(index)" :disabled="disabled">
        {{ option }}
      </button>
    </li>
  </ol>
</template>

<script lang="ts">
import _ from "lodash";
import { PropType, defineComponent } from "vue";

export default defineComponent({
  props: {
    quizQuestion: {
      type: Object as PropType<Generated.QuizQuestion>,
      required: true,
    },
    correctChoiceIndex: Number,
    answerChoiceIndex: Number,
    disabled: Boolean,
  },
  emits: ["answer-to-ai-question"],
  components: {},
  computed: {
    rawJsonQuestion() {
      return this.quizQuestion.rawJsonQuestion;
    },
    aiQuestion() {
      return JSON.parse(this.rawJsonQuestion) as Generated.AIGeneratedQuestion;
    },
    background() {
      return this.aiQuestion.background;
    },
    questionDescription() {
      return this.quizQuestion.description;
    },
    choices() {
      return this.aiQuestion.choices;
    },
  },
  methods: {
    selectOption(optionIndex: number) {
      this.$emit("answer-to-ai-question", optionIndex);
    },
    isSelectedOption(optionIndex: number) {
      return this.answerChoiceIndex === optionIndex;
    },
    isOptionCorrect(index: number) {
      return index === this.correctChoiceIndex;
    },
  },
});
</script>

<style lang="scss" scoped>
ol {
  li {
    padding: 15px;
  }
}

.is-correct {
  font-weight: bold;
  background-color: #00ff00;
}

.is-incorrect {
  font-weight: bold;
  background-color: #ff0000;
}
</style>
