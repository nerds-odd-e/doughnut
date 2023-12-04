<template>
  <div>
    <ul>
      <li
        v-for="(
          { questionFromAI, answerFromUser }, index
        ) in completionInProgress.clarifyingHistory"
        :key="index"
      >
        {{ questionFromAI }}
        <ul>
          <li>{{ answerFromUser }}</li>
        </ul>
      </li>
    </ul>
  </div>
  <form @submit.prevent="handleFormSubmit">
    <h3>
      Clarification question by the AI:
      <strong>{{ completionInProgress.question }}</strong>
    </h3>
    <TextInput
      scope-name="note"
      field="answerToAI"
      v-model="answerToAI"
      v-focus
    />
    <input type="submit" value="Send" class="btn btn-primary" />
  </form>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import TextInput from "../form/TextInput.vue";

export default defineComponent({
  props: {
    completionInProgress: {
      type: Object as PropType<Generated.AiCompletion>,
      required: true,
    },
  },
  components: { TextInput },
  emits: ["submit"],
  data() {
    return {
      answerToAI: "",
    };
  },
  methods: {
    handleFormSubmit() {
      this.$emit("submit", this.answerToAI);
    },
  },
});
</script>

<style lang="scss" scoped></style>
