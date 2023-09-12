import openai
import os
openai.api_key = os.getenv("OPENAI_API_TOKEN")

openai.FineTuningJob.create(training_file="file-DWBJ9D8lNnbWfsF8ErBEC86g", model="gpt-3.5-turbo")
