import HttpResponseError from "./HttpResponseError";
import BadRequestError from "./BadRequestError";

const loginOrRegister = () => {
  window.location = `/users/identify?from=${window.location.href}`;
};

function objectToFormData(data) {
  const formData = new FormData();
  Object.keys(data).forEach((key) => {
    if (data[key] === null) {
      formData.append(key, '');
    } else if (data[key] instanceof Object && !(data[key] instanceof File)) {
      Object.keys(data[key]).forEach((subKey) => {
        formData.append(
          `${key}.${subKey}`,
          data[key][subKey] === null ? '' : data[key][subKey]
        );
      });
    } else {
      formData.append(key, data[key]);
    }
  });
  return formData;
}

const request = async (url, data, {method="GET", contentType='json'}) => {
  const headers = {Accept: 'application/json' };
  let body;
  if (method !== "GET") {
    if (contentType === 'json') {
      headers["Content-Type"] = 'application/json';
      body = JSON.stringify(data)
    }
    else {
      body = objectToFormData(data)
    }
  }
  const res = await fetch(url, {method, headers, body})
  if (res.status === 200 || res.status === 400) {
    return res;
  }
  if (res.status === 204) {
    return {json: ()=>null, text: ()=>null};
  }
  if (res.status === 401) {
    loginOrRegister();
  }
  throw new HttpResponseError(res.status);
}

class Api {
  constructor(base_url) {
    this.base_url = base_url
    const expanUrl = (url) => {
      if(url.startsWith("/")) return url;
      return this.base_url + url;
    }

    this.restRequest = async (url, data, params) => {
      const response = await request(expanUrl(url), data, params);
      const jsonResponse = await response.json()
      if (response.status === 400) throw new BadRequestError(jsonResponse.errors);
      return jsonResponse;
    }

    this.restRequestWithHtmlResponse = async (url, data, params) => {
      const response = await request(expanUrl(url), data, params)
      const html = await response.text();
      if (response.status === 400) throw Error("BadRequest", html);
      return html;
    }
  }

  restGet(url) { return this.restRequest(url, {}, {}); }

  restPost(url, data) { return this.restRequest( url, data, { method: 'POST' }); }

  restPatch(url, data) { return this.restRequest( url, data, { method: 'PATCH' }); }

  restPostMultiplePartForm(url, data) {
     return this.restRequest( url, data, { method: 'POST', contentType: "MultiplePartForm" });
  }

  restPatchMultiplePartForm(url, data) {
    return this.restRequest( url, data, { method: 'PATCH', contentType: "MultiplePartForm" });
  }

  restPostWithHtmlResponse(url, data) {
    return this.restRequestWithHtmlResponse(url, data, { method: 'POST'});
  }
}

export {
  Api,
  loginOrRegister,
};
