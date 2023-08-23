<template>
  <div class="alert alert-success" v-if="answeredQuestion.correct">
    Correct!
  </div>
  <div class="alert alert-danger" v-else>
    <strong>
      {{ "Your answer `" + answeredQuestion.answerDisplay + "` is incorrect." }}
    </strong>
    <a
      class="accordion-button"
      type="button"
      data-bs-toggle="collapse"
      data-bs-target="#incorrectReasonArea"
      aria-expanded="true"
      aria-controls="incorrectReasonArea"
    >
      why my answer is wrong?
    </a>
    <div
      id="incorrectReasonArea"
      class="accordion-collapse collapse"
      aria-labelledby="headingOne"
      data-bs-parent="#accordionExample"
    >
      <div class="accordion-body" id="incorrectReason">test message.</div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";

export default defineComponent({
  props: {
    answeredQuestion: {
      type: Object as PropType<Generated.AnsweredQuestion>,
      required: true,
    },
  },
});
</script>
