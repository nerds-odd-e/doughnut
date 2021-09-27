<template>
  <NoteCardWithHeaderAndButton :notes="notes">
    <template v-if="owns" #headerButtons="{ note }">
      <NoteButtons
        :note="note"
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
  components: { NoteCardWithHeaderAndButton, NoteButtons },
};
</script>
