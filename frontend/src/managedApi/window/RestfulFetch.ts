import HttpResponseError from "./HttpResponseError";
import BadRequestError from "./BadRequestError";
import loginOrRegisterAndHaltThisThread from "./loginOrRegisterAndHaltThisThread";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type JsonData = any;

type HttpMethod = "GET" | "POST" | "PATCH" | "DELETE";

function objectToFormData(data: JsonData) {
  const formData = new FormData();
  Object.keys(data).forEach((key) => {
    if (data[key] === null) {
      formData.append(key, "");
    } else if (data[key] instanceof Object && !(data[key] instanceof File)) {
      Object.keys(data[key]).forEach((subKey) => {
        formData.append(
          `${key}.${subKey}`,
          data[key][subKey] === null ? "" : data[key][subKey],
        );
      });
    } else {
      formData.append(key, data[key]);
    }
  });
  return formData;
}

interface RequestOptions {
  method: HttpMethod;
  contentType?: "json" | "MultiplePartForm";
}

const request = async (
  url: string,
  data: JsonData | undefined,
  { method, contentType = "json" }: RequestOptions,
): Promise<Response> => {
  const headers = new Headers();
  headers.set("Accept", "*/*");
  let body: string | FormData | undefined;
  if (method !== "GET" && data) {
    if (contentType === "json") {
      headers.set("Content-Type", "application/json");
      body = JSON.stringify(data);
    } else {
      body = objectToFormData(data);
    }
  }
  const res = await fetch(url, { method, headers, body });
  if (res.status === 200 || res.status === 204 || res.status === 400) {
    return res;
  }
  if (res.status === 401) {
    if (method === "GET") {
      await loginOrRegisterAndHaltThisThread();
    }
    if (
      // eslint-disable-next-line no-alert
      window.confirm(
        "You are logged out. Do you want to log in (and lose the current changes)?",
      )
    ) {
      await loginOrRegisterAndHaltThisThread();
    }
  }
  let errorMsg = "";

  try {
    const resMsg = await res.json();
    errorMsg = resMsg.message;
  } catch (e) {
    throw new HttpResponseError(res.status, res.statusText);
  }
  throw new HttpResponseError(res.status, errorMsg);
};

class RestfulFetch {
  baseUrl: string;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  private expandUrl(url: string): string {
    if (url.startsWith("/")) return url;
    return this.baseUrl + url;
  }

  async restRequest(url: string, data: JsonData, params: RequestOptions) {
    const response = await request(this.expandUrl(url), data, params);
    try {
      const jsonResponse = await response.json();
      if (response.status === 400) throw new BadRequestError(jsonResponse);
      return jsonResponse;
    } catch (e) {
      return {};
    }
  }

  async restRequestWithHtmlResponse(
    url: string,
    data: JsonData,
    params: RequestOptions,
  ) {
    const response = await request(this.expandUrl(url), data, params);
    if (response.status === 400) throw Error("BadRequest");
    return response.text();
  }
}

export default RestfulFetch;
export type { JsonData, HttpMethod };
