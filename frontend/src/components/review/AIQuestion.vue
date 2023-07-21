<template>
  <p v-if="background">
    {{ background }}
  </p>
  <h3>
    {{ questionDescription }}
  </h3>
  <ol v-if="options" type="A">
    <li
      v-for="(option, index) in options"
      :key="index"
      :class="{
        'is-correct': isSelectedOption(index) && isOptionCorrect(option),
        'is-incorrect': isSelectedOption(index) && !isOptionCorrect(option),
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
import { defineComponent } from "vue";

export default defineComponent({
  props: {
    rawJsonQuestion: { type: String, required: true },
    disabled: Boolean,
  },
  emits: ["answer-to-ai-question"],
  components: {},
  data() {
    return {
      selectedOptionIndex: undefined as number | undefined,
    };
  },
  computed: {
    aiQuestion() {
      return JSON.parse(this.rawJsonQuestion) as Generated.AIGeneratedQuestion;
    },
    background() {
      return this.aiQuestion.background;
    },
    questionDescription() {
      return this.aiQuestion.stem;
    },
    correctOption() {
      return this.aiQuestion.correctChoice;
    },
    options() {
      return _.shuffle([
        ...this.aiQuestion.incorrectChoices,
        this.aiQuestion.correctChoice,
      ]);
    },
  },
  methods: {
    selectOption(optionIndex: number) {
      this.selectedOptionIndex = optionIndex;
      this.$emit("answer-to-ai-question", this.options[optionIndex]);
    },
    isSelectedOption(optionIndex: number) {
      return this.selectedOptionIndex === optionIndex;
    },
    isOptionCorrect(option?: string) {
      return option === this.correctOption;
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
