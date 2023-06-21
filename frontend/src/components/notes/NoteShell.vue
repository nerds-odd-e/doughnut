<template>
  <div class="note-body" :style="`border-color: ${bgColor}`">
    <slot />
    <div class="note-footer" :style="`background-color: ${bgColor}`">
      <slot name="footer" />
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  props: {
    id: Number,
    updatedAt: String,
  },
  computed: {
    bgColor() {
      const colorOld = [150, 150, 150];
      const newColor = [208, 237, 23];
      const ageInMillisecond = Math.max(
        0,
        /* eslint-disable  @typescript-eslint/no-non-null-assertion */
        Date.now() - new Date(this.updatedAt!).getTime()
      );
      const max = 15; // equals to 225 hours
      const index = Math.min(max, Math.sqrt(ageInMillisecond / 1000 / 60 / 60));
      return `rgb(${colorOld
        .map((oc, i) =>
          Math.round((oc * index + newColor[i]! * (max - index)) / max)
        )
        .join(",")})`;
    },
  },
});
</script>

<style scoped>
.note-body {
  padding-left: 10px;
  padding-right: 10px;
  border-radius: 10px;
  border-style: solid;
  border-top-width: 3px;
  border-bottom-width: 1px;
  border-right-width: 3px;
  border-left-width: 1px;
}

.note-footer {
  padding-left: 13px;
  padding-right: 13px;
  border-bottom-left-radius: 10px;
  border-bottom-right-radius: 10px;
  margin-left: -10px;
  margin-right: -13px;
}
</style>
