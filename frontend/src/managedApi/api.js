import ManagedApi from "./ManagedApi";

const api = (component) => {
  const managedApi = new ManagedApi(component);
  return {
    userMethods: {
      logout() {
        return managedApi.restPostWithHtmlResponse(`/logout`, {});
      },

      currentUser() {
          return managedApi.restGet(`user`);
      },
    },
    reviewMethods: {
      processAnswer(reviewPointId, data) {
        return managedApi.restPost(`reviews/${reviewPointId}/answer`, data);
      },

      removeFromReview(reviewPointId) {
        return managedApi.restPost(`review-points/${reviewPointId}/remove`, {});
      },

      overview() {
          return managedApi.restGet(`reviews/overview`);
      },

      getReviewSetting(noteId) {
          return managedApi.restGet(`notes/${noteId}/review-setting`);
      },

      updateReviewSetting(noteId, data) {
          return managedApi.restPost(`notes/${noteId}/review-setting`, data);
      }
    },
    circleMethods: {
        createCircle(data) {
            return managedApi.restPostMultiplePartForm("circles", data);
        },
        joinCircle(data) {
            return managedApi.restPostMultiplePartForm( `circles/join`, data)
        },
        getCirclesOfCurrentUser() {
            return managedApi.restGet("circles");
        },
    },

    relativeSearch(noteId, {searchGlobally, searchKey}) {
        return managedApi.restPost(
          `notes/${noteId}/search`,
          { searchGlobally, searchKey })
    },

    updateNotebookSettings(notebookId, data) {
      return managedApi.restPostMultiplePartForm(
        `notebooks/${notebookId}`, data
      )
    },

    getBazaar() {
        return managedApi.restGet("bazaar");
    },
    shareToBazaar(notebookId) {
        return managedApi.restPost( `notebooks/${notebookId}/share`, {})
    },

    getFailureReports() {
        return managedApi.restGet("failure-reports");
    },
    getFailureReport(failureReportId) {
        return managedApi.restGet(`failure-reports/${failureReportId}`);
    },
    subscriptionMethods: {
        subscribe(notebookId, data) {
        return managedApi.restPostMultiplePartForm(
            `subscriptions/notebooks/${notebookId}/subscribe`, data
        )
        },
        updateSubscription(subscriptionId, data) {
        return managedApi.restPostMultiplePartForm(
            `subscriptions/${subscriptionId}`, data
        )
        },
        deleteSubscription(subscriptionId) {
            return managedApi.restPost(
            `subscriptions/${subscriptionId}/delete`,
            {},
            )
        },
    },
    getStatistics(noteId, linkId) {
        return managedApi.restGet(`${noteId ? `notes/${noteId}`: `links/${linkId}`}/statistics`);
    }
  };
}

export default api;
