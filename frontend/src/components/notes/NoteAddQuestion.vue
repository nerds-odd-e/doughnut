<template>
  <div>
    <label for="question">Question:</label>
    <TextArea id="question" rows="2" v-model="question" /><br />

    <div v-for="(_, index) in options" :key="index">
      <div v-if="index == 0">
        <label :for="'option' + (index + 1)">Option 1 (Correct Answer)</label>
        <TextArea
          :id="'option' + (index + 1)"
          :rows="1"
          v-model="options[index]"
        />
      </div>
      <div v-else>
        <label :for="'option' + (index + 1)">Option {{ index + 1 }}</label>
        <TextArea
          :id="'option' + (index + 1)"
          :rows="1"
          v-model="options[index]"
        />
      </div>
      <br />
    </div>

    <button @click="addOption" :disabled="options.length >= maxOptions">
      +
    </button>
    <button @click="removeOption" :disabled="options.length <= minOptions">
      -
    </button>
    <button @click="submitQuestions" :disabled="isInvalidQuestion">
      Submit
    </button>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import useLoadingApi from "@/managedApi/useLoadingApi";
import TextArea from "../form/TextArea.vue";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    noteId: { type: Number, required: true },
  },
  data() {
    return {
      question: "", // Initialize the question data property
      options: ["", ""], // Initialize with two options
      minOptions: 2, // Minimum number of options
      maxOptions: 10, // Maximum number of options
      placeHolder: { type: "Correct Answer", default: "-" },
    };
  },
  computed: {
    isInvalidQuestion() {
      // Check if any question or option is null or empty
      if (this.question.trim().length === 0) {
        return true;
      }
      return this.options.some((option) => option.trim().length === 0);
    },
  },
  methods: {
    addOption() {
      if (this.options.length < this.maxOptions) {
        this.options.push("");
      }
    },
    removeOption() {
      if (this.options.length > this.minOptions) {
        this.options.pop();
      }
    },
    submitQuestions() {},
  },
});
</script>
