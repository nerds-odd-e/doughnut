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
    processAnswer(reviewPointId: Doughnut.ID, data: Generated.Answer) {
      return managedApi.restPost(`reviews/${reviewPointId}/answer`, data);
    },

    removeFromReview(reviewPointId: Doughnut.ID) {
      return managedApi.restPost(`review-points/${reviewPointId}/remove`, {});
    },

    overview() {
      return managedApi.restGet(`reviews/overview`);
    },

    getReviewSetting(noteId: Doughnut.ID) {
      return managedApi.restGet(`notes/${noteId}/review-setting`);
    },

    updateReviewSetting(noteId: Doughnut.ID, data: Generated.ReviewSetting) {
      return managedApi.restPost(`notes/${noteId}/review-setting`, data);
    },
  },
  circleMethods: {
    createCircle(data: Generated.Circle) {
      return managedApi.restPostMultiplePartForm('circles', data);
    },
    joinCircle(data: Generated.CircleJoiningByInvitation) {
      return managedApi.restPostMultiplePartForm(`circles/join`, data);
    },
    getCirclesOfCurrentUser() {
      return managedApi.restGet('circles');
    },
  },

  relativeSearch(noteId: Doughnut.ID, searchTerm: Generated.SearchTerm) {
    return managedApi.restPost(`notes/${noteId}/search`, searchTerm);
  },

  updateNotebookSettings(notebookId: Doughnut.ID, data: Generated.Notebook) {
    return managedApi.restPostMultiplePartForm(`notebooks/${notebookId}`, data);
  },

  getBazaar() {
    return managedApi.restGet('bazaar');
  },
  shareToBazaar(notebookId: Doughnut.ID) {
    return managedApi.restPost(`notebooks/${notebookId}/share`, {});
  },

  getFailureReports() {
    return managedApi.restGet('failure-reports');
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
    updateSubscription(subscriptionId: Doughnut.ID, data: Generated.Subscription) {
      return managedApi.restPostMultiplePartForm(
        `subscriptions/${subscriptionId}`,
        data
      );
    },
    deleteSubscription(subscriptionId: Doughnut.ID) {
      return managedApi.restPost(`subscriptions/${subscriptionId}/delete`, {});
    },
  },
  getStatistics(noteId: Doughnut.ID | undefined, linkId: Doughnut.ID | undefined) {
    return managedApi.restGet(
      `${noteId ? `notes/${noteId}` : `links/${linkId}`}/statistics`
    );
  },
});

export default apiCollection;
