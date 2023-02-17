import HttpResponseError from "./HttpResponseError";
import BadRequestError from "./BadRequestError";
import loginOrRegisterAndHaltThisThread from "./loginOrRegisterAndHaltThisThread";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type JsonData = any;

type HttpMethod = "GET" | "POST" | "PATCH";

function objectToFormData(data: JsonData) {
  const formData = new FormData();
  Object.keys(data).forEach((key) => {
    if (data[key] === null) {
      formData.append(key, "");
    } else if (data[key] instanceof Object && !(data[key] instanceof File)) {
      Object.keys(data[key]).forEach((subKey) => {
        formData.append(
          `${key}.${subKey}`,
          data[key][subKey] === null ? "" : data[key][subKey]
        );
      });
    } else {
      formData.append(key, data[key]);
    }
  });
  return formData;
}

function parseMultijson(multijson: string) {
  let isEscaped = false;
  let lastRootBracket = 0;
  let inString = false;
  let bracketDepth = 0;
  let char: string;
  for (let i = 0; i < multijson.length; i += 1) {
    if (isEscaped) {
      isEscaped = false;
      // eslint-disable-next-line no-continue
      continue;
    }
    char = multijson.charAt(i);

    if (char === "\\") {
      if (inString) {
        isEscaped = true;
      } else {
        throw new Error("Invalid json: backslash outside string");
      }
    } else if (char === '"') {
      inString = !inString;
    } else if (!inString) {
      if (char === "{") {
        if (bracketDepth === 0) {
          lastRootBracket = i;
        }
        bracketDepth += 1;
      } else if (char === "}") {
        bracketDepth -= 1;
        if (bracketDepth < 0) {
          throw new Error("Invalid JSON: unbalanced brackets");
        }
      }
    }
  }

  if (bracketDepth !== 0) {
    throw new Error("Invalid JSON: unbalanced brackets");
  }

  return JSON.parse(multijson.substring(lastRootBracket));
}
interface RequestOptions {
  method: HttpMethod;
  contentType?: "json" | "MultiplePartForm";
}

const request = async (
  url: string,
  data: JsonData | undefined,
  { method, contentType = "json" }: RequestOptions
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
    await loginOrRegisterAndHaltThisThread();
  }
  throw new HttpResponseError(res.status);
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
    const gen = this.restRequest1(url, data, params);
    const item = gen.next();
    return (await item).value;
  }

  async *restRequest1(url: string, data: JsonData, params: RequestOptions) {
    const response = await request(this.expandUrl(url), data, params);
    if (response.status === 400)
      throw new BadRequestError(await response.json());
    const reader = response.body?.getReader();
    let done = false;
    while (reader && !done) {
      // eslint-disable-next-line no-await-in-loop
      const { done: isDone, value } = await reader.read();
      done = done || isDone;
      const jsonString = new TextDecoder("utf-8").decode(value);
      try {
        yield JSON.parse(jsonString);
      } catch (_e) {
        yield parseMultijson(jsonString);
      }
    }
  }

  async restRequestWithHtmlResponse(
    url: string,
    data: JsonData,
    params: RequestOptions
  ) {
    const response = await request(this.expandUrl(url), data, params);
    if (response.status === 400) throw Error("BadRequest");
    return response.text();
  }
}

export default RestfulFetch;
export type { JsonData, HttpMethod };
