<template>
  <div class="loading-bar">
    <div>
      <content-loader
        height="2"
        :speed="1"
        primary-color="#00c"
      ></content-loader>
    </div>
  </div>
</template>

<script>
import { ContentLoader } from "vue-content-loader";

export default {
  components: {
    ContentLoader,
  },
};
</script>
<style scoped>
.loading-bar {
  position: relative;
}
.loading-bar > div {
  position: absolute;
  width: 100%;
  height: 2px;
  top: -12px;
}
</style>
