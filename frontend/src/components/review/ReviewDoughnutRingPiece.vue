<template>
  <g>
    <path
      :id="`${name}-curve`"
      :d="`M${startPoint},A40 40 0 ${bigArc ? 1 : 0} 1,${endPoint}`"
      fill="none"
      stroke-width="20"
    />
    <title>{{ title }}</title>
    <text>
      <textPath
        startOffset="50%"
        text-anchor="middle"
        alignment-baseline="middle"
        :xlink:href="`#${name}-curve`"
      >
        {{ text }}
      </textPath>
    </text>
  </g>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  props: {
    name: String,
    title: String,
    bigArc: Boolean,
    startPoint: String,
    endPoint: String,
    text: String,
  },
});
</script>
