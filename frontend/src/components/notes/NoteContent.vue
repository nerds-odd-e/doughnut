<template>
  <div class="alert alert-warning" v-if="note.deletedAt">
    This note has been deleted
  </div>
  <NoteTextContent
    :note-id="note.id"
    :text-content="note.textContent"
    :size="size"
    :storage-accessor="storageAccessor"
  >
    <template #title-additional>
      <div class="header-options">
        <NoteWikidataAssociation
          v-if="note.wikidataId"
          :wikidata-id="note.wikidataId"
        />
      </div>
    </template>

    <template #note-content-other>
      <template v-if="note.pictureWithMask">
        <ShowPicture
          v-if="size !== 'small'"
          class="text-center"
          v-bind="note.pictureWithMask"
          :opacity="0.2"
        />
        <SvgPictureIndicator v-else class="picture-indicator" />
      </template>
      <template v-if="!!note.noteAccessories.url">
        <div v-if="size != 'small'">
          <label
            id="note-url"
            v-text="note.noteAccessories.urlIsVideo ? 'Video Url:' : 'Url:'"
          />
          <a
            aria-labelledby="note-url"
            :href="note.noteAccessories.url"
            target="_blank"
            >{{ note.noteAccessories.url }}</a
          >
        </div>
        <a v-else :href="note.noteAccessories.url" target="_blank">
          <SvgUrlIndicator />
        </a>
      </template>
    </template>
  </NoteTextContent>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ShowPicture from "./ShowPicture.vue";
import SvgPictureIndicator from "../svgs/SvgPictureIndicator.vue";
import SvgUrlIndicator from "../svgs/SvgUrlIndicator.vue";
import NoteWikidataAssociation from "./NoteWikidataAssociation.vue";
import type { StorageAccessor } from "../../store/createNoteStorage";
import NoteTextContent from "./NoteTextContent.vue";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
    size: { type: String, default: "large" },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    ShowPicture,
    SvgPictureIndicator,
    SvgUrlIndicator,
    NoteWikidataAssociation,
    NoteTextContent,
  },
});
</script>

<style lang="sass" scoped>
.header-options
  display: flex
  align-items: center
  margin: 0 10px
</style>
