<template>
  <ContainerPage v-bind="{ loading: false, contentExists: true }">
    <h1 v-if="!!user" class="display-4">Welcome {{ user.name }}!</h1>
    <h1 v-else class="display-4">Welcome to Doughnut!!!</h1>

    <div class="row">
      <div class="col-sm-6">
        <div class="card text-white bg-success mb-3">
          <div class="card-header">Notes</div>
          <div class="card-body">
            <p class="card-text">
              Browse and manage your notes. You can add, reorg and build links
              between your notes.
            </p>
            <p v-if="!user">Please login</p>
            <router-link
              class="btn btn-light"
              v-else
              :to="{ name: 'notebooks' }"
            >
              Go To My Notes
            </router-link>
          </div>
        </div>
      </div>
      <div class="col-sm-6">
        <div class="card text-white bg-primary mb-3">
          <div class="card-header">Review</div>
          <div class="card-body">
            <p class="card-text">
              Review your notes before you are about to forget. Test your memory
              and update your notes with new realizations.
            </p>
            <p v-if="!user">Please login</p>
            <router-link class="btn btn-light" v-else :to="{ name: 'reviews' }">
              Start Review
            </router-link>
          </div>
        </div>
      </div>
      <div class="col-sm-6">
        <div class="card text-white bg-danger mb-3">
          <div class="card-header">Bazaar</div>
          <div class="card-body">
            <p class="card-text">
              Look at the notes shared by other people. Search for interesting
              content to learn.
            </p>
            <router-link class="btn btn-light" :to="{ name: 'bazaar' }">
              Visit The Bazaar
            </router-link>
          </div>
        </div>
      </div>
      <div class="col-sm-6">
        <div class="card text-black bg-light mb-3">
          <div class="card-header">Circles</div>
          <div class="card-body">
            <p class="card-text">
              Create and join circles. You can own notes together within a
              circle.
            </p>
            <p v-if="!user">Please login</p>
            <PopupButton
              class="btn btn-light"
              title="choose a circle"
              :sidebar="true"
            >
              <template #button_face> Go To Circles </template>
              <template #dialog_body="{ doneHandler }">
                <CircleSelector @done="doneHandler($event)" />
              </template>
            </PopupButton>
          </div>
        </div>
      </div>
    </div>
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import PopupButton from "@/components/commons/Popups/PopupButton.vue";
import CircleSelector from "@/components/circles/CircleSelector.vue";
import ContainerPage from "./commons/ContainerPage.vue";

export default defineComponent({
  props: {
    user: {
      type: Object as PropType<Generated.User>,
      required: false,
    },
  },
  components: { ContainerPage, PopupButton, CircleSelector },
});
</script>
