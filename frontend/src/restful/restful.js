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

const restRequest = (url, params, loadingRef, callback, errorCallback) => {
  if (loadingRef instanceof Function) {loadingRef(true)}
  else { loadingRef.value = true }
  fetch(url, params)
    .then(res => {
      if (res.status !== 200 && res.status !=400) {
          throw new HttpResponseError(res.status)
      }
      return res.json().then(resp => {
        if(res.status === 200) callback(resp)
        if(res.status === 400) errorCallback(resp.errors)
      })
    })
    .catch(error => {
        if(error.status === 401) {
          reloadSession()
          return
        }
        window.alert(error)
    })
    .finally(()=>{
      if (loadingRef instanceof Function) {loadingRef(false)}
      else { loadingRef.value = false }
    });
}

const restGet = (url, loadingRef, callback) => {
    restRequest(url, {}, loadingRef, callback, ()=>{})
}

const restPost = (url, data, loadingRef, callback) => {
    restRequest(url, {
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    }, loadingRef, callback, ()=>{})
}

const restPostMultiplePartForm = (url, data, loadingRef, callback, errorCallback) => {
    const formData = new FormData()
    Object.keys(data).forEach(function(key,index) {
      formData.append(key, data[key] === null ? "" : data[key])
    });

    restRequest(url, {
        method: 'POST',
        headers: {
          'Accept': 'application/json',
        },
        body: formData
    }, loadingRef, callback, errorCallback)
}

export {restGet, restPost, restPostMultiplePartForm}