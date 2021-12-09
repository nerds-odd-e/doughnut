<template>
      <div class="note-body" @dblclick="editDialog" :style="`border-color: ${bgColor}`">
        <slot/>
      </div>
</template>

<script>
import { editNote } from "../dialogs";

export default {
  props: {
    id: [String, Number],
    updatedAt: String,
    language: String,
    isEditingTitle: Boolean,
  },
  components: {
  },
  computed: {
    bgColor() {
      const colorOld = [150, 150, 150];
      const newColor = [208, 237, 23];
      const ageInMillisecond = Math.max(
        0,
        Date.now() - new Date(this.updatedAt)
      );
      const max = 15; // equals to 225 hours
      const index = Math.min(max, Math.sqrt(ageInMillisecond / 1000 / 60 / 60));
      return `rgb(${colorOld
        .map((oc, i) =>
          Math.round((oc * index + newColor[i] * (max - index)) / max)
        )
        .join(",")})`;
    },
  },
  methods: {
    async editDialog() {
      if (!this.isEditingTitle) {
        await editNote(this.$popups, this.id, this.language);
      }
    },
  },
};
</script>
