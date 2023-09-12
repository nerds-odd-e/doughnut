import openai
import os
openai.api_key = os.getenv("OPENAI_API_TOKEN")

print(openai.FineTuningJob.list(limit=10))
