<template>
  <div class="btn-group" role="group" aria-label="First group">
    <button
      class="btn btn-light"
      id="repeat-reset"
      name="reset"
      @click="processForm('reset')"
      title="reset the interval to mininum"
    >
      <SvgFailed />
    </button>
    <button
      class="btn btn-light"
      id="repeat-sad"
      name="sad"
      @click.once="processForm('sad')"
      title="reduce next repeat interval (days) by half"
    >
      <SvgSad />
    </button>
    <button
      class="btn btn-light"
      id="repeat-satisfied"
      name="satisfying"
      @click="processForm('satisfying')"
      title="use normal repeat interval (days)"
    >
      <SvgSatisfying />
    </button>
    <button
      class="btn btn-light"
      id="repeat-happy"
      name="happy"
      @click="processForm('happy')"
      title="add to next repeat interval (days) by half"
    >
      <SvgHappy />
    </button>
  </div>
</template>

<script>
import SvgSad from "../svgs/SvgSad.vue";
import SvgSatisfying from "../svgs/SvgSatisfying.vue";
import SvgFailed from "../svgs/SvgFailed.vue";
import SvgHappy from "../svgs/SvgHappy.vue";

export default {
  emits: ["selfEvaluate"],
  components: { SvgSad, SvgSatisfying, SvgFailed, SvgHappy },
  methods: {
    processForm(evaluation) {
      this.$emit("selfEvaluate", evaluation);
    },
  },
};
</script>
