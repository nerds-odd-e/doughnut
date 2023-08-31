import openai
import os
openai.api_key = os.getenv("OPENAI_API_KEY")

response = openai.File.create(
  file=open("training_data.jsonl", "rb"),
  purpose='fine-tune'
)

# Extract the file ID from the response
file_id = response['id']
file_name = response['filename']

# Print or use the file ID and filename
print(f"File ID: {file_id}")
print(f"File Name: {file_name}")
