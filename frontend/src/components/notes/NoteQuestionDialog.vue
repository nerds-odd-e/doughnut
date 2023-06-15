<template>
  <h2>Generate a question</h2>
  <form>
    <div v-if="question">
      {{ question }}
    </div>
    <ol v-if="options.length > 0" type="A">
      <li>{{ options[0].option }}</li>
      <li>{{ options[1].option }}</li>
      <li>{{ options[2].option }}</li>
    </ol>
  </form>
  <button class="btn btn-secondary" @click="generateQuestion">Ask again</button>
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
    };
  },
  methods: {
    async generateQuestion() {
      const aiAdvisor = new AiAdvisor(this.selectedNote.textContent);
      const prompt = aiAdvisor.questionPrompt();
      const res = await this.api.ai.askAiSuggestions({
        prompt,
      });
      const parsed: AiQuestion = JSON.parse(res.suggestion);

      this.question = parsed.question;
      this.options = parsed.options;

      if (res.finishReason === "length") {
        await this.generateQuestion();
      }
    },
  },
  mounted() {
    this.generateQuestion();
  },
});
</script>

<style lang="scss" scoped>
.ai-art {
  width: 100%;
  height: 100%;
}
</style>
