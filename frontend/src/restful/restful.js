import HttpResponseError from "./HttpResponseError";
import BadRequestError from "./BadRequestError";

const loginOrRegister = () => {
  window.location = `/users/identify?from=${window.location.href}`;
};

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

const restRequest = async (url, data, params) => {
  const response = await request(url, data, params);
  const jsonResponse = await response.json()
  if (response.status === 400) throw new BadRequestError(jsonResponse.errors);
  return jsonResponse;
}

const restRequestWithHtmlResponse = async (url, data, params) => {
  const response = await request(url, data, params)
  const html = await response.text();
  if (response.status === 400) throw Error("BadRequest", html);
  return html;
}

const restGet = (url) => restRequest(url, {}, {});

const restPost = (url, data) =>
  restRequest(
    url,
    data,
    {
      method: 'POST',
    },
  );

const restPatch = (url, data) =>
  restRequest( url, data, { method: 'PATCH' },
  );

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

const restPostMultiplePartForm = (url, data) =>
  restRequest( url, data, { method: 'POST', contentType: "MultiplePartForm" });

const restPatchMultiplePartForm = (url, data) =>
  restRequest( url, data, { method: 'PATCH', contentType: "MultiplePartForm" });

const restPostWithHtmlResponse = (url, data) =>
  restRequestWithHtmlResponse(url, data, { method: 'POST'});

export {
  restGet,
  restPost,
  restPatch,
  restPostMultiplePartForm,
  restPatchMultiplePartForm,
  loginOrRegister,
  restPostWithHtmlResponse,
};
