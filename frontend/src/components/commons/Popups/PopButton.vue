<template>
  <a class="btn btn-sm" role="button" @click="show = true" :title="title">
    <slot name="button_face" />
    <template v-if="!$slots.button_face">
      {{ title }}
    </template>
  </a>
  <Popup v-if="show" :sidebar="sidebar" @popup-done="show = false">
    <slot />
  </Popup>
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue";
import Popup from "./Popup.vue";

export default defineComponent({
  props: { title: String, sidebar: String as PropType<"left" | "right"> },
  data() {
    return { show: false };
  },
  components: { Popup },
});
</script>
