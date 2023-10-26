import { beforeEach, describe, expect, it, vitest } from "vitest";
import downloadJSONL, {
  toJSONL,
} from "../../../src/components/admin/downloadJSONL";

describe("downloadJSONL", () => {
  const examples = [
    {
      messages: [{ role: "user", content: "cnt", function_call: null }],
    } as unknown as Generated.OpenAIChatGPTFineTuningExample,
  ];

  const element = {
    click: vitest.fn(),
    tagName: "a",
    download: "",
    // add any other necessary properties here
  } as unknown as HTMLAnchorElement;

  beforeEach(() => {
    // Mock relevant functions and properties
    global.URL.createObjectURL = vitest.fn();
    global.URL.revokeObjectURL = vitest.fn();

    document.createElement = vitest.fn(() => element);
  });

  it("should create and click an anchor element with correct blob data", () => {
    downloadJSONL(examples, "test.jsonl");

    expect(global.URL.createObjectURL).toHaveBeenCalled();
    expect(element.download).toBe("test.jsonl");
    expect(element.click).toHaveBeenCalled();
    expect(global.URL.revokeObjectURL).toHaveBeenCalled();
  });

  it("should return a stringified JSONL", () => {
    const result = toJSONL(examples);

    expect(result).toBe('{"messages":[{"role":"user","content":"cnt"}]}');
  });
});
