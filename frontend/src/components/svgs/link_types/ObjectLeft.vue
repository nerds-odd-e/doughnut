<template>
  <svg
    viewBox="0 0 100 100"
    :x="x"
    :y="y ? y : 25"
    :height="!!oheight ? oheight : 350"
  >
    <path
      d="
  M75,5 a1,1 0 0,0 0,90
  a1,1 0 0,0 0,-45
  a1,1 0 0,1 0,-45
  "
      :fill="fill"
    />
  </svg>
</template>

<script setup>
import { computed } from "vue";
import { bgcolors } from "../../../colors";

const props = defineProps({
  linkRole: String,
  x: Number,
  y: Number,
  oheight: Number,
});
const fill = computed(() => bgcolors(props.linkRole));
</script>
