<template>
  <h3>
    {{ question.question }}
  </h3>
  <ol v-if="question.options" type="A">
    <li
      v-for="(option, index) in question.options"
      :key="index"
      @click="selectOption(index)"
      :class="{
        'is-correct': isSelectedOption(index) && option.correct,
        'is-wrong': isSelectedOption(index) && !option.correct,
      }"
    >
      {{ option.option }}
    </li>
  </ol>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { AiQuestionModel } from "@/models/AiAdvisor";

export default defineComponent({
  props: {
    rawJsonQuestion: { type: String, required: true },
  },
  components: {},
  data() {
    return {
      question: JSON.parse(this.rawJsonQuestion) as AiQuestionModel,
      selectedOptionIndex: undefined as number | undefined,
    };
  },
  methods: {
    selectOption(optionIndex: number) {
      this.selectedOptionIndex = optionIndex;
    },
    isSelectedOption(optionIndex: number) {
      return this.selectedOptionIndex === optionIndex;
    },
  },
});
</script>

<style lang="scss" scoped>
.is-correct {
  font-weight: bold;
  background-color: #00ff00;
}

.is-wrong {
  font-weight: bold;
  background-color: #ff0000;
}
</style>
