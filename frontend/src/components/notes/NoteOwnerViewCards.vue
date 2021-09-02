<template>
  <NoteCardWithHeaderAndButton :notes="notes">
    <template v-if="owns" #headerButtons="{ note }">
      <NoteButtons
        :note="note"
        @updated="$emit('updated')"
        @deleted="$emit('updated')"
      />
    </template>
  </NoteCardWithHeaderAndButton>
</template>

<script>
import NoteCardWithHeaderAndButton from "./NoteCardWithHeaderAndButton.vue";
import NoteButtons from "./NoteButtons.vue";

export default {
  name: "NoteOwnerViewCards",
  props: { notes: Array, owns: Boolean },
  emits: ["updated"],
  components: { NoteCardWithHeaderAndButton, NoteButtons },
};
</script>
