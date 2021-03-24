apt_auto_remote:
  cmd.run:
    - name: apt-get -y update && apt-get -y upgrade && apt-get -y autoremove
