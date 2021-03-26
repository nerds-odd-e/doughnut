doughnut_app_dir:
  file.directory:
    - name: {{ pillar['doughnut_app']['app_dir'] }}
    - makedirs: True
