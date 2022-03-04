import ManagedApi from "./ManagedApi";

const api = (component) => {
  const managedApi = new ManagedApi(component);
  return {
    userMethods: {
      logout() {
        return managedApi.restPostWithHtmlResponse(`/logout`, {});
      },

      currentUser() {
          return managedApi.restGet(`/api/user`);
      },
    },
    reviewMethods: {
      processAnswer(reviewPointId, data) {
        return managedApi.restPost(`/api/reviews/${reviewPointId}/answer`, data);
      },

      removeFromReview(reviewPointId) {
        return managedApi.restPost(`/api/review-points/${reviewPointId}/remove`, {});
      },

      overview() {
          return managedApi.restGet(`/api/reviews/overview`);
      },

      getReviewSetting(noteId) {
          return managedApi.restGet(`/api/notes/${noteId}/review-setting`);
      },

      updateReviewSetting(noteId, data) {
          return managedApi.restPost(`/api/notes/${noteId}/review-setting`, data);
      }
    },
    circleMethods: {
        createCircle(data) {
            return managedApi.restPostMultiplePartForm("/api/circles", data);
        },
        joinCircle(data) {
            return managedApi.restPostMultiplePartForm( `/api/circles/join`, data)
        },
        getCirclesOfCurrentUser() {
            return managedApi.restGet("/api/circles");
        },
    },

    relativeSearch(noteId, {searchGlobally, searchKey}) {
        return managedApi.restPost(
          `/api/notes/${noteId}/search`,
          { searchGlobally, searchKey })
    },

    updateNotebookSettings(notebookId, data) {
      return managedApi.restPostMultiplePartForm(
        `/api/notebooks/${notebookId}`, data
      )
    },

    getBazaar() {
        return managedApi.restGet("/api/bazaar");
    },
    shareToBazaar(notebookId) {
        return managedApi.restPost( `/api/notebooks/${notebookId}/share`, {})
    },

    getFailureReports() {
        return managedApi.restGet("/api/failure-reports");
    },
    getFailureReport(failureReportId) {
        return managedApi.restGet(`/api/failure-reports/${failureReportId}`);
    },
    subscriptionMethods: {
        subscribe(notebookId, data) {
        return managedApi.restPostMultiplePartForm(
            `/api/subscriptions/notebooks/${notebookId}/subscribe`, data
        )
        },
        updateSubscription(subscriptionId, data) {
        return managedApi.restPostMultiplePartForm(
            `/api/subscriptions/${subscriptionId}`, data
        )
        },
        deleteSubscription(subscriptionId) {
            return managedApi.restPost(
            `/api/subscriptions/${subscriptionId}/delete`,
            {},
            )
        },
    },
    getStatistics(noteId, linkId) {
        return managedApi.restGet(`/api/${noteId ? `notes/${noteId}`: `links/${linkId}`}/statistics`);
    }
  };
}

export default api;
