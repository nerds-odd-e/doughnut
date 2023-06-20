<template>
  <div id="note-question-dialog">
    <h2>Generate a question</h2>
    <form>
      <div v-if="question">
        {{ question }}
      </div>
      <ol v-if="options.length > 0" type="A">
        <li
          v-for="(option, index) in options"
          :key="index"
          @click="selectOption(index)"
          :class="{
            'selected-option': isSelectedOption(index),
            'is-correct': isSelectedOption(index) && isCorrectOption(option),
            'is-wrong': isSelectedOption(index) && !isCorrectOption(option),
          }"
        >
          {{ option.option }}
        </li>
      </ol>
    </form>
    <button
      class="btn btn-secondary"
      @click="generateQuestionAndResetSelectedOption"
    >
      Ask again
    </button>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import type { StorageAccessor } from "@/store/createNoteStorage";
import AiAdvisor, {
  AiQuestionOptionType as AiQuestionOption,
  AiQuestionType as AiQuestion,
} from "@/models/AiAdvisor";
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
      question: "",
      options: [] as AiQuestionOption[],
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
      const parsed: AiQuestion = JSON.parse(res.suggestion);

      this.question = parsed.question;
      this.options = parsed.options;

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
    isCorrectOption(optionObject: AiQuestionOption) {
      return optionObject.correct;
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
