<template>
  <h2 v-if="rawJsonQuestion === undefined">Generating question...</h2>
  <div v-else>
    <AIQuestion :raw-json-question="rawJsonQuestion" :key="numberOfTries" />
  </div>
  <button class="btn btn-secondary" @click="generateQuestion">
    Doesn't make sense?
  </button>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import type { StorageAccessor } from "@/store/createNoteStorage";
import useLoadingApi from "../../managedApi/useLoadingApi";
import AIQuestion from "../review/AIQuestion.vue";

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
  components: { AIQuestion },
  data() {
    return {
      quizQuestion: undefined as Generated.QuizQuestion | undefined,
      numberOfTries: 0,
      isUnmounted: false,
    };
  },
  computed: {
    rawJsonQuestion() {
      return this.quizQuestion?.rawJsonQuestion;
    },
  },
  methods: {
    async generateQuestion() {
      this.quizQuestion = await this.api.ai.askAIToGenerateQuestion(
        this.selectedNote.id
      );
      this.numberOfTries += 1;
    },
  },
  mounted() {
    this.generateQuestion();
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
