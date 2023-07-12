<template>
  <div class="alert alert-success" v-if="answerResult.correct">Correct!</div>
  <div class="alert alert-danger" v-else>
    {{ "Your answer `" + answerResult.answerDisplay + "` is incorrect." }}
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";

export default defineComponent({
  props: {
    answerResult: {
      type: Object as PropType<Generated.AnswerViewedByUser>,
      required: true,
    },
  },
});
</script>
