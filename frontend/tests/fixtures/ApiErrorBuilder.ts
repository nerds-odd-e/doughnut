import { ApiError } from "@/generated/backend";
import { ApiRequestOptions } from "@/generated/backend/core/ApiRequestOptions";
import { ApiResult } from "@/generated/backend/core/ApiResult";
import Builder from "./Builder";

class ApiErrorBuilder extends Builder<ApiError> {
  request: ApiRequestOptions = {
    url: "",
    method: "GET",
  };

  response: ApiResult = {
    url: "",
    ok: false,
    status: 404,
    statusText: "not found",
    body: "not found",
  };

  message: string = "not found";

  error404(): ApiErrorBuilder {
    return this;
  }

  do() {
    return new ApiError(this.request, this.response, this.message);
  }
}

export default ApiErrorBuilder;
