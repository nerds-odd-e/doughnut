<template>
    <NoteOwnerBreadcrumb v-bind="{ancestors: noteViewedByUser.ancestors, notebook: noteViewedByUser.notebook}"/>
    <Card :note="noteViewedByUser.note">
        <template #button="{note}">
            <button class="source_btn btn btn-sm btn-secondary" data-toggle="collapse"
             :data-target="`#note-collapse-${note.id}`" aria-expanded="false">
                Toggle Details
            </button>
        </template>
    </Card>
    <div :id="`note-collapse-${noteViewedByUser.note.id}`" class="collapse">
        <NoteShow v-bind="{note: noteViewedByUser.note, links: noteViewedByUser.links, owns: noteViewedByUser.owns}" :level="2"/>
    </div>
</template>

<script>
import NoteOwnerBreadcrumb from "../notes/NoteOwnerBreadcrumb.vue"
import NoteShow from "../notes/NoteShow.vue"
import Card from "../notes/Card.vue"

export default {
  props: { noteViewedByUser: Object },
  components: { NoteOwnerBreadcrumb, NoteShow, Card }

}
</script>
