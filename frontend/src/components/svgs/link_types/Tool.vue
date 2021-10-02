<template>
  <svg viewBox="0 -20 400 440" :x="x" :y="y || 30" height="340" :fill="fill">
    <path
      d="m358.37374,183.04955c28.02139,-32.21715 33.65386,-76.18248 18.41026,-113.157l-56.25531,65.60144l-55.4539,-10.78157l-18.37459,-53.41797l56.11303,-65.44908c-38.59784,-9.71068 -81.1209,2.01269 -109.01885,34.10236c-29.42188,33.83034 -34.2368,80.67877 -16.02618,118.68837l-146.63451,168.62342c-15.60265,17.92763 -13.70185,45.11841 4.2259,60.71496c17.92774,15.58556 45.11309,13.70184 60.71032,-4.22598l146.46273,-168.45706c40.31681,12.99477 86.28311,1.74231 115.84112,-32.24187l-0.00001,-0.00001z"
    />
  </svg>
</template>

<script setup>
import { computed } from "@vue/runtime-core";
import { bgcolors } from "../../../colors";

const props = defineProps({ linkRole: String, x: Number, y: Number });
const fill = computed(() => bgcolors(props.linkRole));
const strokeDashArray = computed(() => (props.abstract ? "20, 12" : null));
</script>
