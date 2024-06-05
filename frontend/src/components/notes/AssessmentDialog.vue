<template>
  <div>
    <div v-if="currentQuestion === 1">
      <p role="question">Where in the world is Singapore?</p>
      <button @click="nextQuestion">Asia</button>
    </div>
    <div v-else-if="currentQuestion === 2">
      <p role="question">Most famous food of Vietnam?</p>
      <button @click="nextQuestion">Pho</button>
    </div>
    <div v-else>
      <p>End of questions</p>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      currentQuestion: 1,
    };
  },
  methods: {
    nextQuestion() {
      if (this.currentQuestion < 2) {
        this.currentQuestion += 1;
      }
    },
  },
};
</script>
