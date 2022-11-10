<template>
  <template v-if="suggestedTitle">
    <label>Suggested Title: {{ suggestedTitle }}</label>
    <RadioButtons
      v-model="replaceOrAppendTitle"
      scope-name="titleRadio"
      :options="[
        { value: 'Replace', label: 'Replace title' },
        { value: 'Append', label: 'Append title' },
      ]"
      @update:model-value="updateModelValue"
    />
  </template>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import RadioButtons from "../form/RadioButtons.vue";

export default defineComponent({
  components: {
    RadioButtons,
  },
  props: {
    originalTitle: { type: String, required: true },
    suggestedTitle: { type: String, required: true },
  },
  emits: ["suggestedTitleSelected"],
  data() {
    return {
      replaceOrAppendTitle: "",
    };
  },
  methods: {
    updateModelValue() {
      if (this.replaceOrAppendTitle === "Replace") {
        this.$emit("suggestedTitleSelected", this.suggestedTitle);
      }

      if (this.replaceOrAppendTitle === "Append") {
        const newTitle = `${this.originalTitle} / ${this.suggestedTitle}`;
        this.$emit("suggestedTitleSelected", newTitle);
      }
    },
  },
});
</script>
