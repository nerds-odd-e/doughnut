<template>
  <h2>Generate a question</h2>
  <div v-if="question">
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
  </div>
  <button
    class="btn btn-secondary"
    @click="generateQuestionAndResetSelectedOption"
  >
    Ask again
  </button>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import type { StorageAccessor } from "@/store/createNoteStorage";
import AiAdvisor, { AiQuestion } from "@/models/AiAdvisor";
import useLoadingApi from "../../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    selectedNote: { type: Object as PropType<Generated.Note>, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: false,
    },
  },
  components: {},
  data() {
    return {
      question: undefined as AiQuestion | undefined,
      selectedOptionIndex: undefined as number | undefined,
      isUnmounted: false,
    };
  },
  methods: {
    async generateQuestion(prev?: string) {
      const aiAdvisor = new AiAdvisor(this.selectedNote.textContent);
      const prompt = aiAdvisor.questionPrompt();
      const res = await this.api.ai.keepAskingAISuggestionUntilStop(
        prompt,
        this.selectedNote.id,
        prev,
        (_) => !this.isUnmounted
      );

      this.question = JSON.parse(res);
    },

    async generateQuestionAndResetSelectedOption() {
      this.selectedOptionIndex = undefined;
      this.generateQuestion();
    },
    selectOption(optionIndex: number) {
      this.selectedOptionIndex = optionIndex;
    },
    isSelectedOption(optionIndex: number) {
      return this.selectedOptionIndex === optionIndex;
    },
  },
  mounted() {
    this.generateQuestionAndResetSelectedOption();
  },
  unmounted() {
    this.isUnmounted = true;
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
