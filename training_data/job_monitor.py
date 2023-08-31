import openai
import os
openai.api_key = os.getenv("OPENAI_API_KEY")

print(openai.FineTuningJob.list(limit=10))
