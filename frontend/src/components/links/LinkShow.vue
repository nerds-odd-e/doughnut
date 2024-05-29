<template>
  <div class="row">
    <div class="col-12 col-md-4 link-source">
      <LinkNoteShow v-if="link.sourceNote" :note="link.sourceNote" />
    </div>
    <div class="col-12 col-md-4 text-center">
      <LinkNob v-bind="{ link, storageAccessor }" />
      <span class="badge bg-light text-dark"> {{ link.note?.linkType }}</span>
    </div>
    <div class="col-12 col-md-4 link-target">
      <LinkNoteShow v-if="link.targetNote" :note="link.targetNote" />
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Thing } from "@/generated/backend";
import LinkNoteShow from "./LinkNoteShow.vue";
import LinkNob from "./LinkNob.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  props: {
    link: {
      type: Object as PropType<Thing>,
      required: true,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: { LinkNoteShow, LinkNob },
});
</script>
