<script lang="ts">
import { defineComponent } from "vue";
import Popups from "./components/commons/Popups/Popups.vue";
import TestMenu from "./components/commons/TestMenu.vue";
import UserNewRegisterPage from "./pages/UserNewRegisterPage.vue";
import useLoadingApi from "./managedApi/useLoadingApi";
import usePopups from "./components/commons/Popups/usePopup";
import ReviewDoughnut from "./components/review/ReviewDoughnut.vue";
import LoginButton from "./components/toolbars/LoginButton.vue";
import NoteControlCenter from "./components/toolbars/NoteControlCenter.vue";
import { sanitizeViewTypeName } from "./models/viewTypes";
import createHistory, { HistoryState } from "./store/history";

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
      updatedNoteRealm: undefined as undefined | Generated.NoteRealm,
      updatedAt: undefined as undefined | Date,
      featureToggle: false,
      environment: "production",
      histories: createHistory(),
    };
  },

  components: {
    Popups,
    TestMenu,
    UserNewRegisterPage,
    ReviewDoughnut,
    LoginButton,
    NoteControlCenter,
  },

  watch: {
    $route() {
      this.popups.done(false);
    },
  },

  computed: {
    newUser() {
      return !this.user && !!this.externalIdentifier;
    },
    viewType() {
      return sanitizeViewTypeName(this.$route.meta.viewType as string);
    },
  },

  methods: {
    historyWriter(writer: (h: HistoryState) => void) {
      writer(this.histories);
    },
    onNoteDeleted(parentId: Doughnut.ID) {
      if (parentId) {
        this.$router.push({
          name: "noteShow",
          params: { noteId: parentId },
        });
      } else {
        this.$router.push({ name: "notebooks" });
      }
    },
    onUpdateNoteRealm(updatedNoteRealm: Generated.NoteRealm) {
      this.updatedNoteRealm = updatedNoteRealm;
      this.updatedAt = new Date();
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
        <template v-if="$route.meta['useControlCenter']">
          <NoteControlCenter
            class="header"
            :selected-note-id="Number($route.params.noteId)"
            v-bind="{ viewType, historyWriter, histories }"
            @note-realm-updated="onUpdateNoteRealm($event)"
            @note-deleted="onNoteDeleted($event)"
          />
          <router-view
            :updated-note-realm="updatedNoteRealm"
            :updated-at="updatedAt"
            :history-writer="historyWriter"
          />
        </template>
        <router-view v-else-if="$route.meta['userProp']" :user="user" />
        <router-view v-else />
      </template>
      <ReviewDoughnut v-if="user" :user="user" @update-user="user = $event" />
      <LoginButton v-else />
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
