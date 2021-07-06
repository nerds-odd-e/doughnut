<template>
  <div class="row">
      <div class="col-12 col-sm-6 col-md-4 col-lg-3" v-for="notebook in notebooks" :key="notebook.id">
          <Card :note="notebook.headNote">
            <template #cardHeader>
              <span class="card-header d-flex flex-row-reverse p-0">
                <slot :notebook="notebook"/>
              </span>
            </template>
          </Card>
      </div>
  </div>
</template>

<script>
import Card from "../notes/Card.vue"
export default {
  name: 'NotebookCardsWithButtons',
  props: {notebooks: Array},
  components: { Card },
}
</script>
