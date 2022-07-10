<template>
  <svg
    id="dropdownMenuButton"
    data-bs-toggle="dropdown"
    data-toggle="dropdown"
    aria-haspopup="true"
    aria-expanded="false"
    title="more options"
    class="doughnut-ring"
    viewBox="-50 -50 100 100"
    width="100"
    height="100"
  >
    <UserIconMenu
      role="button"
      aria-label="User actions"
      class="user-icon-menu"
      style="pointer-events: visiblePainted"
    />
  </svg>

  <div
    class="dropdown-menu dropdown-menu-end"
    aria-labelledby="dropdownMenuButton"
  >
    <router-link to="/settings">Settings</router-link>
    <a href="#" class="dropdown-item" role="button" @click="$emit('logout')"
      >Logout</a
    >
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import UserIconMenu from "./UserIconMenu.vue";

export default defineComponent({
  props: {
    user: {
      type: Object as PropType<Generated.User>,
      required: true,
    },
  },
  emits: ["logout"],
  components: { UserIconMenu },
});
</script>

<style lang="scss" scoped>
.doughnut-ring {
  pointer-events: none;
  font-size: 0.8rem;
  font-weight: bold;
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
}
</style>
