const downloadJSONL = (
  fineTuningData: Generated.OpenAIChatGPTFineTuningExample[],
  filename: string,
) => {
  const blob = new Blob([toJSONL(fineTuningData)], {
    type: "text/plain",
  });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
};

function toJSONL(
  fineTuningData: Generated.OpenAIChatGPTFineTuningExample[],
): BlobPart {
  return fineTuningData
    .map((x) =>
      JSON.stringify(x, (_, value) => {
        return value === null ? undefined : value;
      }),
    )
    .join("\n");
}

export default downloadJSONL;
export { toJSONL };
