<template>
  <h3>
    Clarification question by the AI: <strong>{{ question }}</strong>
  </h3>
  <form>
    <TextInput
      scope-name="note"
      field="answerToAI"
      v-model="answerToAI"
      v-focus
    />
    <input type="submit" value="Send" class="btn btn-primary" />
  </form>
</template>

<script>
import TextInput from "../form/TextInput.vue";

export default {
  props: {
    question: string
  },
  components: { TextInput },
  data() {
    return {
      answerToAI: string
    };
  },
  methods: {},
};
</script>

<style lang="scss" scoped>

</style>