import openai
import os
openai.api_key = os.getenv("OPENAI_API_KEY")

openai.File.create(
  file=open("training_data.json", "rb"),
  purpose='fine-tune'
)
