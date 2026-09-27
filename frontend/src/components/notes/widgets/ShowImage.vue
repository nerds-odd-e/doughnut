<template>
  <div class="note-image text-center" v-if="!!noteImage">
    <div style="position: relative; display: inline-block" id="note-image">
      <img :src="noteImage" />
      <svg
        v-if="!!imageMask"
        viewBox="0 0 100 100"
        style="
          position: absolute;
          top: 0;
          left: 0;
          color: #11f1f1;
          width: 100%;
          height: 100%;
        "
      >
        <template v-for="item in getMasks()" :key="item.index">
          <rect
            :x="item.x"
            :y="item.y"
            :width="item.width"
            :height="item.height"
            style="fill: blue; stroke: pink; stroke-width: 1; fill-opacity: 0.2; stroke-opacity: 0.8"
          />
        </template>
      </svg>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, type PropType } from "vue"
import type { Note, ShowNoteImageData } from "@generated/donut-backend-api"
import { client } from "@generated/donut-backend-api/client.gen"
import { noteImageScalarsFromMarkdown } from "@/utils/noteContentFrontmatter"

const props = defineProps({
  note: { type: Object as PropType<Note>, required: true },
})

/**
 * `<img>` source for a note's `image:` value. An absolute path or a URL is used as-is; anything
 * else is a notebook file relative to the note's folder.
 */
const noteImageSource = (noteId: number, image: string) => {
  if (image.startsWith("/") || /^[a-z][a-z0-9+.-]*:/i.test(image)) return image
  return client.buildUrl<ShowNoteImageData>({
    url: "/api/notes/{note}/image",
    path: { note: noteId },
    query: { path: image },
  })
}

const scalars = computed(() =>
  noteImageScalarsFromMarkdown(props.note.content ?? "")
)
const noteImage = computed(
  () =>
    scalars.value.noteImage &&
    noteImageSource(props.note.id, scalars.value.noteImage)
)
const imageMask = computed(() => scalars.value.imageMask)

const createGroups = (arr: string[], perGroup: number): string[][] => {
  const numGroups = Math.ceil(arr.length / perGroup)
  return new Array(numGroups)
    .fill("")
    .map((_, i) => arr.slice(i * perGroup, (i + 1) * perGroup))
}

const getMasks = () => {
  if (!imageMask.value) return []
  return createGroups(imageMask.value.split(/\s+/), 4).map((arr, index) => {
    const [x, y, width, height] = arr
    return { index, x, y, width, height }
  })
}
</script>

<style lang="sass" scoped>
.note-image
  width: 100%
  height: 100%
  img
    max-width: 100%
    max-height: 100%
</style>
