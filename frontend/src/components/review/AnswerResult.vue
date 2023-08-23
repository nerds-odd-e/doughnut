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
      data-bs-target="#collapseOne"
      aria-expanded="true"
      aria-controls="collapseOne"
    >
      why my answer is wrong?
    </a>
    <div
      id="collapseOne"
      class="accordion-collapse collapse"
      aria-labelledby="headingOne"
      data-bs-parent="#accordionExample"
    >
      <div class="accordion-body">test message.</div>
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
