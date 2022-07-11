<template>
  <nav class="vertical-menu">
    <div class="menu">
      <router-link class="nav-link" :to="{ name: 'testability' }"
        >Testability</router-link
      >
      <div v-if="featureToggle" class="nav-item">
        <em class="nav-link btn-danger">Feature Toggle is On </em>
      </div>
    </div>
  </nav>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";

export default defineComponent({
  props: {
    featureToggle: Boolean,
    user: Object as PropType<Generated.User>,
  },
});
</script>

<style lang="scss" scoped>
.vertical-menu {
  pointer-events: none;
  position: fixed;
  right: 0px;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  width: 60px;
  font-size: x-small;

  .menu {
    pointer-events: initial;
    background-color: yellow;
  }
}
</style>
