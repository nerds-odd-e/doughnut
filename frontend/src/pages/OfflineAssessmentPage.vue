<template>
  <h3>Assessment For {{ topicConstructor }}</h3>
  <OnlineAssessmentDialog :notebook-id="notebookId" />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import useLoadingApi from "@/managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return { ...useLoadingApi() };
  },
  props: {
    notebookId: { type: Number, required: true },
  },
  computed: {
    topicConstructor() {
      return this.$route.query.topic;
    },
  },
});
</script>
