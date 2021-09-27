<template>
  <span class="link-link">
    <LinkNob
      v-bind="{ link, colors }"
      v-if="!!reverse"
      :inverseIcon="true"
    />
    <NoteTitleWithLink class="link-title" v-bind="{ note }" />
    <LinkNob
      v-bind="{ link, colors }"
      v-if="!reverse"
      :inverseIcon="false"
    />
  </span>
</template>

<script setup>
import { computed } from "@vue/runtime-core";
import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue";
import LinkNob from "./LinkNob.vue";

const props = defineProps({
  link: Object,
  reverse: Boolean,
  colors: { Object, required: true },
});
const note = computed(() =>
  !!props.reverse ? props.link.sourceNote : props.link.targetNote
);
const fontColor = computed(() =>
  !!props.reverse ? props.colors["target"] : props.colors["source"]
);
</script>

<style scoped>
.link-link {
  padding-bottom: 3px;
  margin-right: 10px;
}

.link-title {
  padding-bottom: 3px;
  color: v-bind(fontColor);
}
</style>
