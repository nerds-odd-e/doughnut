<template>
  <div>
    <div>Questions here</div>
  </div>
</template>

<script>
export default {
  // data function defines initial state of the component
  data() {
    return {
      currentQuestion: 1,
    };
  },
  // method property defines methods that can be called within the component.
  methods: {
    nextQuestion() {
      if (this.currentQuestion < 2) {
        this.currentQuestion += 1;
      }
    },
  },
};
</script>
