import ManagedApi from "./ManagedApi";

const apiCollection = (managedApi: ManagedApi) => ({
  userMethods: {
    logout() {
      return managedApi.restPostWithHtmlResponse(`/logout`, {});
    },

    currentUser() {
      return managedApi.restGet(`user`);
    },
  },
  reviewMethods: {
    removeFromReview(reviewPointId: Doughnut.ID) {
      return managedApi.restPost(`review-points/${reviewPointId}/remove`, {});
    },

    overview() {
      return managedApi.restGet(`reviews/overview`);
    },

    async getReviewSetting(noteId: Doughnut.ID) {
      return (await managedApi.restGet(
        `notes/${noteId}/review-setting`
      )) as Generated.ReviewSetting;
    },

    updateReviewSetting(
      noteId: Doughnut.ID,
      data: Omit<Generated.ReviewSetting, "id">
    ) {
      return managedApi.restPost(`notes/${noteId}/review-setting`, data);
    },
  },
  circleMethods: {
    createCircle(data: Generated.Circle) {
      return managedApi.restPostMultiplePartForm("circles", data);
    },
    joinCircle(data: Generated.CircleJoiningByInvitation) {
      return managedApi.restPostMultiplePartForm(`circles/join`, data);
    },
    getCirclesOfCurrentUser() {
      return managedApi.restGet("circles");
    },
  },

  async relativeSearch(searchTerm: Generated.SearchTerm) {
    return (await managedApi.restPost(
      `notes/search`,
      searchTerm
    )) as Generated.Note[];
  },

  updateNotebookSettings(notebookId: Doughnut.ID, data: Generated.Notebook) {
    return managedApi.restPostMultiplePartForm(`notebooks/${notebookId}`, data);
  },

  getBazaar() {
    return managedApi.restGet("bazaar");
  },
  shareToBazaar(notebookId: Doughnut.ID) {
    return managedApi.restPost(`notebooks/${notebookId}/share`, {});
  },

  getFailureReports() {
    return managedApi.restGet("failure-reports");
  },
  getFailureReport(failureReportId: Doughnut.ID) {
    return managedApi.restGet(`failure-reports/${failureReportId}`);
  },
  subscriptionMethods: {
    subscribe(notebookId: Doughnut.ID, data: Generated.Subscription) {
      return managedApi.restPostMultiplePartForm(
        `subscriptions/notebooks/${notebookId}/subscribe`,
        data
      );
    },
    updateSubscription(
      subscriptionId: Doughnut.ID,
      data: Generated.Subscription
    ) {
      return managedApi.restPostMultiplePartForm(
        `subscriptions/${subscriptionId}`,
        data
      );
    },
    deleteSubscription(subscriptionId: Doughnut.ID) {
      return managedApi.restPost(`subscriptions/${subscriptionId}/delete`, {});
    },
  },
  getStatistics(
    noteId: Doughnut.ID | undefined,
    linkId: Doughnut.ID | undefined
  ) {
    return managedApi.restGet(
      `${noteId ? `notes/${noteId}` : `links/${linkId}`}/statistics`
    );
  },

  async getNotebooks() {
    return (await managedApi.restGet(
      `notebooks`
    )) as Generated.NotebooksViewedByUser;
  },

  comments: {
    createNoteComment(noteId: Doughnut.ID, content: Generated.CommentCreation) {
      return managedApi.restPost(`/api/notes/${noteId}/createComment`, content);
    },

    async getNoteComments(noteId: Doughnut.ID) {
      return (await managedApi.restGet(
        `/api/notes/${noteId}/comments`
      )) as Generated.Comment[];
    },
  },
});

export default apiCollection;
