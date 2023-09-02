<template>
  <ToolbarFrame>
    <UserIconMenu
      role="button"
      aria-label="User actions"
      class="user-icon-menu"
      data-bs-toggle="dropdown"
      data-toggle="dropdown"
      aria-haspopup="true"
      aria-expanded="false"
    />

    <div
      v-if="user"
      class="dropdown-menu dropdown-menu-end"
      aria-labelledby="dropdownMenuButton"
    >
      <PopButton class="dropdown-item" title="choose a circle">
        <template #button_face> Settings for {{ user.name }}</template>
        <UserProfileDialog
          @user-updated="
            if ($event) {
              $emit('updateUser', $event);
            }
          "
        />
      </PopButton>
      <a href="#" class="dropdown-item" role="button" @click="logout">Logout</a>
    </div>
  </ToolbarFrame>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import UserIconMenu from "./UserIconMenu.vue";
import PopButton from "../commons/Popups/PopButton.vue";
import UserProfileDialog from "./UserProfileDialog.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import ToolbarFrame from "./ToolbarFrame.vue";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    user: { type: Object as PropType<Generated.User> },
  },
  emits: ["updateUser"],
  components: {
    UserIconMenu,
    PopButton,
    UserProfileDialog,
    ToolbarFrame,
  },
  methods: {
    async logout() {
      await this.api.userMethods.logout();
      window.location.href = "/bazaar";
    },
  },
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
