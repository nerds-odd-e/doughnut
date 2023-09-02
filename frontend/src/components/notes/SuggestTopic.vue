<template>
  <template v-if="suggestedTopic">
    <label>Suggested Topic: {{ suggestedTopic }}</label>
    <RadioButtons
      v-model="replaceOrAppendTopic"
      scope-name="topicRadio"
      :options="[
        { value: 'Replace', label: 'Replace topic' },
        { value: 'Append', label: 'Append topic' },
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
    originalTopic: { type: String, required: true },
    suggestedTopic: { type: String, required: true },
  },
  emits: ["suggestedTopicSelected"],
  data() {
    return {
      replaceOrAppendTopic: "",
    };
  },
  methods: {
    updateModelValue() {
      if (this.replaceOrAppendTopic === "Replace") {
        this.$emit("suggestedTopicSelected", this.suggestedTopic);
      }

      if (this.replaceOrAppendTopic === "Append") {
        const newTopic = `${this.originalTopic} / ${this.suggestedTopic}`;
        this.$emit("suggestedTopicSelected", newTopic);
      }
    },
  },
});
</script>
