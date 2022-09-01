<script lang="ts">
import { defineComponent } from "vue";
import Popups from "./components/commons/Popups/Popups.vue";
import TestMenu from "./components/commons/TestMenu.vue";
import UserNewRegisterPage from "./pages/UserNewRegisterPage.vue";
import useLoadingApi from "./managedApi/useLoadingApi";
import usePopups from "./components/commons/Popups/usePopup";
import ReviewDoughnut from "./components/review/ReviewDoughnut.vue";
import NoteControlCenter from "./components/toolbars/NoteControlCenter.vue";
import { sanitizeViewTypeName } from "./models/viewTypes";
import createNoteStorage from "./store/createNoteStorage";

export default defineComponent({
  setup() {
    return {
      ...useLoadingApi({ initalLoading: true, skipLoading: true }),
      ...usePopups(),
    };
  },
  data() {
    return {
      externalIdentifier: undefined as undefined | string,
      user: undefined as undefined | Generated.User,
      featureToggle: false,
      environment: "production",
      storageAccessor: createNoteStorage(),
    };
  },

  components: {
    Popups,
    TestMenu,
    UserNewRegisterPage,
    ReviewDoughnut,
    NoteControlCenter,
  },

  watch: {
    $route() {
      this.popups.done(false);
    },
    "storageAccessor.updatedAt": function updatedAt() {
      if (!this.storageAccessor.updatedNoteRealm) {
        this.$router.replace({ name: "notebooks" });
      }
    },
  },

  computed: {
    newUser() {
      return !this.user && !!this.externalIdentifier;
    },
    viewType() {
      return sanitizeViewTypeName(this.$route.meta.viewType as string);
    },
    routeViewProps() {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const props = {} as any;
      if (this.$route.meta.useNoteStorageAccessor) {
        props.storageAccessor = this.storageAccessor;
      }
      if (this.$route.meta.userProp) {
        props.user = this.user;
      }
      return props;
    },
  },

  async mounted() {
    this.environment = this.api.testability.getEnvironment();
    this.featureToggle = await this.api.testability.getFeatureToggle();
    this.api.userMethods
      .getCurrentUserInfo()
      .then((res) => {
        this.user = res.user;
        this.externalIdentifier = res.externalIdentifier;
      })
      .finally(() => (this.loading = false));
  },
});
</script>

<template>
  <div class="box">
    <Popups />
    <UserNewRegisterPage v-if="newUser" @update-user="user = $event" />
    <template v-else>
      <template v-if="!loading">
        <div class="header">
          <NoteControlCenter
            :selected-note-id="Number($route.params.noteId)"
            v-bind="{ viewType, storageAccessor, user }"
            @update-user="user = $event"
          />
        </div>
        <router-view v-bind="routeViewProps" />
      </template>
      <ReviewDoughnut v-if="user" />
      <TestMenu
        v-if="environment === 'testing'"
        :feature-toggle="featureToggle"
        :user="user"
        @feature-toggle="featureToggle = $event"
      />
    </template>
  </div>
</template>

<style lang="sass" scoped>
.box
  display: flex
  flex-flow: column
  height: 100vh

.box .header
  flex: 0 1 auto

.box .content
  flex: 1 1 auto

.box .footer
  flex: 0 1 40px
</style>

<style lang="sass">
.inner-box
  display: flex
  flex-flow: column
  height: 100%

.inner-box .header
  flex: 0 1 auto

.inner-box .content
  flex: 1 1 auto
  overflow: hidden

.inner-box .footer
  flex: 0 1 40px
</style>
