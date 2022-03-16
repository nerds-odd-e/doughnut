<template>
  <NoteShell
    v-if="note"
    class="note-body"
    v-bind="{ id: note.id, updatedAt: note.textContent?.updatedAt }"
  >
    <NoteFrameOfLinks v-bind="{ links }">
      <NoteContent v-bind="{ note }" />
    </NoteFrameOfLinks>
  </NoteShell>
</template>

<script lang="ts">
import { defineComponent, PropType } from 'vue'
import EditableText from "../form/EditableText.vue";
import NoteFrameOfLinks from "../links/NoteFrameOfLinks.vue";
import NoteShell from "./NoteShell.vue";
import NoteContent from "./NoteContent.vue";

export default defineComponent({
  name: "NoteWithLinks",
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
    links: { type: Object as PropType<{ [P in Generated.LinkType]?: Generated.LinkViewed }> },
  },
  components: {
    NoteFrameOfLinks,
    NoteShell,
    NoteContent,
    EditableText,
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

.note-title {
  margin-top: 0px;
  padding-top: 10px;
  color: black;
}

.outdated-label {
  display: inline-block;
  height: 100%;
  vertical-align: middle;
  margin-left: 20px;
  padding-bottom: 10px;
  color: red;
}
</style>
