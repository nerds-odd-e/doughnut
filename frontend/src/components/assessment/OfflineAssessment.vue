<template>
  <h3>Assessment For LeSS in Action</h3>
  <!-- <p>{{ result }}</p>
  <p>{{ errors }}</p>
  <p>{{ notebook.id }}</p> -->
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Notebook } from "@/generated/backend";
import useLoadingApi from "@/managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return { ...useLoadingApi() };
  },
  props: {
    notebook: { type: Object as PropType<Notebook>, required: true },
  },
  data() {
    return {
      result: {},
      errors: {},
    };
  },
  methods: {
    generateAssessmentQuestions() {
      this.managedApi.restAssessmentController
        .generateAiQuestions(this.notebook.id)
        .then((response) => {
          this.result = response;
          this.$router.push({ name: "notebooks" });
        })
        .catch((res) => {
          this.errors = res;
        });
    },
  },
  mounted() {
    this.generateAssessmentQuestions();
  },
});
</script>
