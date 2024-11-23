<template>
  <div class="d-flex sidebar-control">
    <PopButton v-if="user" title="open sidebar" :sidebar="'left'">
      <template #button_face>
        <SvgSidebar />
      </template>
      <GlobalSidebar
        :user="user"
        @update-user="$emit('updateUser', $event)"
      />
    </PopButton>
    <GlobalSidebar
      v-if="user"
      :user="user"
      :iconized="true"
      @update-user="$emit('updateUser', $event)"
    />
    <LoginButton v-else />
  </div>
</template>

<script setup lang="ts">
import type { User } from "@/generated/backend"
import type { PropType } from "vue"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import SvgSidebar from "@/components/svgs/SvgSidebar.vue"
import GlobalSidebar from "@/components/toolbars/GlobalSidebar.vue"
import LoginButton from "@/components/toolbars/LoginButton.vue"

defineProps({
  user: { type: Object as PropType<User> },
})
defineEmits(["updateUser"])
</script>

<style scoped>
.sidebar-control {
  background-color: #f5f5f5;
  flex-direction: column;
  height: 100%;
}

@media (max-width: 768px) {
  .sidebar-control {
    flex-direction: row;
    width: 100%;
    height: auto;
  }
}
</style>
