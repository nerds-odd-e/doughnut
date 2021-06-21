class HttpResponseError extends Error {
    constructor(status) {
      super(`got ${status}`);
      this.status = status;
    }
  }

const reloadSession = () => {
    window.alert("You login session is expired, let's refresh.")
    window.location.reload();
}

const restGet = (url, loadingRef, callback) => {
  if (loadingRef instanceof Function) {loadingRef(true)}
  else { loadingRef.value = true }
  fetch(url)
    .then(res => {
      if (res.status !== 200) {
          throw new HttpResponseError(res.status)
      }
      return res.json();
    })
    .then(resp => {
      callback(resp)
      if (loadingRef instanceof Function) {loadingRef(false)}
      else { loadingRef.value = false }
    })
    .catch(error => {
        if(error.status === 401) {
          reloadSession()
          return
        }
        window.alert(error)
    });
}

export {restGet}