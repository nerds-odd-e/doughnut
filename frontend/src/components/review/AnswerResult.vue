<template>
      <div class="alert alert-success" v-if="answerResult.correct">
        Correct!
      </div>
      <div class="alert alert-danger" v-else>
        {{ "Your answer `" + answerResult.answerDisplay + "` is wrong." }}
      </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";

export default defineComponent({
  name: "Repetition",
  props: {
    answerResult: {type: Object as PropType<Generated.AnswerResult>, required: true},
  },
});
</script>
