import openai
import os
openai.api_key = os.getenv("OPENAI_API_TOKEN")

openai.FineTuningJob.create(training_file="file-y3VUS0uyBFNZKH5zw1r672Jw", model="gpt-3.5-turbo")
