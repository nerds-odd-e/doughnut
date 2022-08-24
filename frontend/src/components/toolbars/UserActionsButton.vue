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
    <PopupButton class="dropdown-item" title="choose a circle" :sidebar="true">
      <template #button_face>All Notes</template>
      <template #dialog_body="{ doneHandler }">
        <CircleSelector @done="doneHandler($event)" />
      </template>
    </PopupButton>
    <router-link class="dropdown-item" :to="{ name: 'reviews' }"
      >Review</router-link
    >
    <div class="dropdown-divider"></div>
    <PopupButton class="dropdown-item" title="choose a circle">
      <template #button_face>{{ user.name }}</template>
      <template #dialog_body="{ doneHandler }">
        <UserProfileDialog
          @done="
            doneHandler($event);
            if ($event) {
              $emit('updateUser', $event);
            }
          "
        />
      </template>
    </PopupButton>
    <a href="#" class="dropdown-item" role="button" @click="$emit('logout')"
      >Logout</a
    >
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import UserIconMenu from "./UserIconMenu.vue";
import PopupButton from "../commons/Popups/PopupButton.vue";
import CircleSelector from "../circles/CircleSelector.vue";
import UserProfileDialog from "./UserProfileDialog.vue";

export default defineComponent({
  props: {
    user: {
      type: Object as PropType<Generated.User>,
      required: true,
    },
  },
  emits: ["updateUser", "logout"],
  components: { UserIconMenu, PopupButton, CircleSelector, UserProfileDialog },
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
