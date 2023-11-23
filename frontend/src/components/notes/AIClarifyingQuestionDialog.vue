<template>
  <form @submit.prevent="handleFormSubmit">
    <h3>
      Clarification question by the AI: <strong>{{ question }}</strong>
    </h3>
    <TextInput
      scope-name="note"
      field="answerToAI"
      v-model="answerToAI"
      v-focus
    />
    <input
      type="submit"
      value="Send"
      class="btn btn-primary"
      data-cy="submit-answer"
    />
  </form>
</template>

<script>
import TextInput from "../form/TextInput.vue";
import asPopup from "../commons/Popups/asPopup";

export default {
  setup() {
    return {
      ...asPopup(),
    };
  },
  props: {
    question: String,
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
      this.popup.done(this.answerToAI);
    },
  },
};
</script>

<style lang="scss" scoped></style>
