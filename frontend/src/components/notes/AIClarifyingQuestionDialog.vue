<template>
  <form @submit.prevent="handleFormSubmit">
    <h3>
      Clarification question by the AI:
      <strong>{{ aiCompletion.question }}</strong>
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
import asPopup from "../commons/Popups/asPopup";

export default defineComponent({
  setup() {
    return {
      ...asPopup(),
    };
  },
  props: {
    aiCompletion: {
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
      this.popup.done(this.answerToAI);
    },
  },
});
</script>

<style lang="scss" scoped></style>
