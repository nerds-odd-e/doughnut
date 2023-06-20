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
          'selected-option': isSelectedOption(index),
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
      selectedOptionIndex: -1 as number,
    };
  },
  methods: {
    async generateQuestion() {
      const aiAdvisor = new AiAdvisor(this.selectedNote.textContent);
      const prompt = aiAdvisor.questionPrompt();
      const res = await this.api.ai.askAiSuggestions(
        {
          prompt,
          incompleteAssistantMessage: "",
        },
        this.selectedNote.id
      );

      this.question = JSON.parse(res.suggestion);

      if (res.finishReason === "length") {
        await this.generateQuestion();
      }
    },
    async generateQuestionAndResetSelectedOption() {
      this.resetSelectedOption();
      this.generateQuestion();
    },
    resetSelectedOption() {
      this.selectOption(-1);
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
});
</script>

<style lang="scss" scoped>
.ai-art {
  width: 100%;
  height: 100%;
}

.selected-option {
  font-weight: bold;
}

.is-correct {
  background-color: #00ff00;
}

.is-wrong {
  background-color: #ff0000;
}
</style>
