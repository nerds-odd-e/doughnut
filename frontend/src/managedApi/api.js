import {
  restGet,
  restPost,
  restPostMultiplePartForm,
  restPostWithHtmlResponse,
} from '../restful/restful';

const api = () => ({
    userMethods: {
      logout() {
        return restPostWithHtmlResponse(`/logout`, {});
      },

      currentUser() {
          return restGet(`/api/user`);
      },
    },
    reviewMethods: {
      processAnswer(reviewPointId, data) {
        return restPost(`/api/reviews/${reviewPointId}/answer`, data);
      },

      removeFromReview(reviewPointId) {
        return restPost(`/api/review-points/${reviewPointId}/remove`, {});
      },

      overview() {
          return restGet(`/api/reviews/overview`);
      },

      getReviewSetting(noteId) {
          return restGet(`/api/notes/${noteId}/review-setting`);
      },

      updateReviewSetting(noteId, data) {
          return restPost(`/api/notes/${noteId}/review-setting`, data);
      }
    },
    circleMethods: {
        createCircle(data) {
            return restPostMultiplePartForm("/api/circles", data);
        },
        joinCircle(data) {
            return restPostMultiplePartForm( `/api/circles/join`, data)
        },
        getCirclesOfCurrentUser() {
            return restGet("/api/circles");
        },
    },

    relativeSearch(noteId, {searchGlobally, searchKey}) {
        return restPost(
          `/api/notes/${noteId}/search`,
          { searchGlobally, searchKey })
    },

    updateNotebookSettings(notebookId, data) {
      return restPostMultiplePartForm(
        `/api/notebooks/${notebookId}`, data
      )
    },

    getBazaar() {
        return restGet("/api/bazaar");
    },
    shareToBazaar(notebookId) {
        return restPost( `/api/notebooks/${notebookId}/share`, {})
    },

    getFailureReports() {
        return restGet("/api/failure-reports");
    },
    getFailureReport(failureReportId) {
        return restGet(`/api/failure-reports/${failureReportId}`);
    },
    subscriptionMethods: {
        subscribe(notebookId, data) {
        return restPostMultiplePartForm(
            `/api/subscriptions/notebooks/${notebookId}/subscribe`, data
        )
        },
        updateSubscription(subscriptionId, data) {
        return restPostMultiplePartForm(
            `/api/subscriptions/${subscriptionId}`, data
        )
        },
        deleteSubscription(subscriptionId) {
            return restPost(
            `/api/subscriptions/${subscriptionId}/delete`,
            {},
            )
        },
    },
  })

export default api;
