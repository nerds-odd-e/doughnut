import json
import os
import tempfile
from openai import OpenAI
import time
import pathspec

# Initialize OpenAI client
client = OpenAI(api_key=os.getenv("OPENAI_API_TOKEN"))

def upload_file(filepath, purpose='assistants'):
    with open(filepath, 'rb') as file:
        response = client.files.create(file=file, purpose=purpose)
    return response.id

def attach_files_to_assistant(assistant_id, file_id):
    # Retrieve the current state of the assistant
    assistant = client.beta.assistants.retrieve(assistant_id=assistant_id)

    vector_store_id = assistant.tool_resources.file_search.vector_store_ids[0]

    # Update the vector store with the new list of file IDs
    response = client.beta.vector_stores.files.create(
        vector_store_id=vector_store_id,
        file_id=file_id,
    )
    return assistant

def find_source_files(root_dir, gitignore_path):
    source_files = []
    ignore_patterns = []

    # Read .gitignore and compile ignore patterns
    with open(gitignore_path, 'r') as gitignore_file:
        ignore_patterns = pathspec.PathSpec.from_lines('gitwildmatch', gitignore_file)

    for root, _, files in os.walk(root_dir):
        for file in files:
            file_path = os.path.join(root, file)
            relative_path = os.path.relpath(file_path, root_dir)

            # Check if the file matches any of the .gitignore patterns
            if ignore_patterns.match_file(relative_path):
                continue  # Skip ignored files

            # Check file size, skip if larger than 500KB
            file_size = os.path.getsize(file_path)
            if file_size > 500 * 1024:  # 500KB
                print(f'File too large, skipping: {file_path}')
                continue

            source_files.append(file_path)

    return source_files

def main(assistant_id, project_dir, gitignore):
    source_files = find_source_files(project_dir, gitignore)
    files = []

    for filepath in source_files:
      try:
        files += [{
          filepath: filepath,
          'content': open(filepath, 'r').read(),
        }]
        # print(f'success file: {filepath}')
      except Exception as e:
        print(f'Binary file: {filepath}')

    # Create a temporary file and write the json of files to it
    with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix="_all_source_code.json") as temp_file:
        json.dump(files, temp_file)
        temp_file_path = temp_file.name

    print(f'Temporary file created at: {temp_file_path}')

    file_id = upload_file(temp_file_path)

    print('Attaching files to the assistant...')
    updated_assistant = attach_files_to_assistant(assistant_id, file_id)
    print('Files successfully attached to the assistant:', updated_assistant.id)

# Set your assistant ID and project directory
assistant_id = 'asst_4wvS7l1MYpjtjV72Ip9l37cs'
project_dir = '../'
gitignore = '../.gitignore'

if __name__ == "__main__":
    main(assistant_id, project_dir, gitignore)
