import openai
import os
openai.api_key = os.getenv("OPENAI_API_KEY")

openai.FineTuningJob.create(training_file="file-Xmy2W19dr4afSV99L1sxE6Gx", model="gpt-3.5-turbo")
