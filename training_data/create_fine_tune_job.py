import openai
import os
openai.api_key = os.getenv("OPENAI_API_KEY")

openai.FineTuningJob.create(training_file="file-VXfYeBO14nuHqcSYh5Xv73ZL", model="gpt-3.5-turbo")
