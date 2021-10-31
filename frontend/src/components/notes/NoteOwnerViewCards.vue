<template>
  <NoteCardWithHeaderAndButton :notes="notes">
    <template v-if="owns" #headerButtons="{ note }">
      <NoteButtons
        :note="note"
        :deleteRedirect="true"
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
