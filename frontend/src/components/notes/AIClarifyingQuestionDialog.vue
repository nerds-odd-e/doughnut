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
    <input type="submit" value="Send" class="btn btn-primary" />
  </form>
</template>

<script>
import TextInput from "../form/TextInput.vue";

export default {
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
    },
  },
};
</script>

<style lang="scss" scoped></style>
