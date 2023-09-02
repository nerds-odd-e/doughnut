<template>
  <div class="dropup w-100">
    <button
      aria-label="User actions"
      class="user-icon-menu btn d-block w-100 text-start"
      data-bs-toggle="dropdown"
      data-toggle="dropdown"
      aria-haspopup="true"
      aria-expanded="false"
    >
      <SvgMissingAvatar :x="-13" :y="-20" :height="40" />
      {{ user.name }}
    </button>
    <div class="dropdown-menu" aria-labelledby="dropdownMenuButton">
      <PopButton class="dropdown-item" title="user settings">
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
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import PopButton from "../commons/Popups/PopButton.vue";
import UserProfileDialog from "./UserProfileDialog.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import SvgMissingAvatar from "../svgs/SvgMissingAvatar.vue";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    user: { type: Object as PropType<Generated.User>, required: true },
  },
  emits: ["updateUser"],
  components: {
    PopButton,
    UserProfileDialog,
    SvgMissingAvatar,
  },
  methods: {
    async logout() {
      await this.api.userMethods.logout();
      window.location.href = "/bazaar";
    },
  },
});
</script>
