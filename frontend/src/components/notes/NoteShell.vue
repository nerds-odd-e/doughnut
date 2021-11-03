<template>
      <div class="note-body" @dblclick="editDialog" :style="`border-color: ${bgColor}`">
        <slot/>
      </div>
</template>

<script>
import NoteEditDialog from "./NoteEditDialog.vue";

export default {
  props: {
    id: [String, Number],
    updatedAt: String,
  },
  components: {
    NoteEditDialog,
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
      await this.$popups.dialog(NoteEditDialog, {
        noteId: this.id,
      })
    },
  },
};
</script>
