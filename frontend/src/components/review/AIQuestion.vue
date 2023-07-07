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
      role="button"
      :key="index"
      @click="selectOption(index)"
      :class="{
        'is-correct': isSelectedOption(index) && isOptionCorrect(option),
        'is-wrong': isSelectedOption(index) && !isOptionCorrect(option),
      }"
    >
      {{ option }}
    </li>
  </ol>
</template>

<script lang="ts">
import _ from "lodash";
import { defineComponent } from "vue";

export default defineComponent({
  props: {
    rawJsonQuestion: { type: String, required: true },
  },
  emits: ["selfEvaluatedMemoryState", "is-question-answered"],
  components: {},
  data() {
    const aiQuestion = JSON.parse(
      this.rawJsonQuestion
    ) as Generated.AIGeneratedQuestion;
    return {
      background: aiQuestion.background,
      questionDescription: aiQuestion.stem,
      correctOption: aiQuestion.correctChoice,
      options: _.shuffle([
        ...aiQuestion.incorrectChoices,
        aiQuestion.correctChoice,
      ]),
      selectedOptionIndex: undefined as number | undefined,
    };
  },
  methods: {
    selectOption(optionIndex: number) {
      this.selectedOptionIndex = optionIndex;
      this.$emit(
        "selfEvaluatedMemoryState",
        this.isOptionCorrect(this.options[optionIndex]) ? "yes" : "no"
      );
      this.$emit("is-question-answered", true);
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

.is-wrong {
  font-weight: bold;
  background-color: #ff0000;
}
</style>
