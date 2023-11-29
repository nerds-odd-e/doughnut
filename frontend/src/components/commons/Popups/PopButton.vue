<template>
  <a class="btn btn-sm" role="button" @click="show = true" :title="title">
    <slot name="button_face" />
    <template v-if="!$slots.button_face">
      {{ title }}
    </template>
    <Popup1 v-if="show" :sidebar="sidebar" @popup-done="show = false">
      <slot />
    </Popup1>
  </a>
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue";
import Popup1 from "./Popup1.vue";

export default defineComponent({
  props: { title: String, sidebar: String as PropType<"left" | "right"> },
  data() {
    return { show: false };
  },
  components: { Popup1 },
});
</script>
