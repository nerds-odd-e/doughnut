<template>
  <g role="button" title="user manu">
    <circle style="fill: #ffc10e" cx="0" cy="0" r="25" />
    <SvgMissingAvatar :x="-13" :y="-20" :height="40" />
  </g>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import SvgMissingAvatar from "../svgs/SvgMissingAvatar.vue";

export default defineComponent({ components: { SvgMissingAvatar } });
</script>
