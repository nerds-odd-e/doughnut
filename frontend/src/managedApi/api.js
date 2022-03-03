import {
  restGet,
  restPost,
  restPostMultiplePartForm,
  restPostWithHtmlResponse,
} from '../restful/restful';

class ManagedApi {
  restGet(url) { return restGet(url); }
  restPost(url, data) { return restPost(url, data);}
  restPostMultiplePartForm(url, data) {return restPostMultiplePartForm(url, data)}
  restPostWithHtmlResponse(url, data) {return restPostWithHtmlResponse(url, data)}
}

const api = () => {
  const managedApi = new ManagedApi();
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
  };
}

export default api;
