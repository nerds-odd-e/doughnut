Here's how to convert a git repository from HTTPS to SSH:

1. Check your current remote URL:
```bash
git remote -v
```
This will show something like:
```
origin  https://github.com/nerds-odd-e/doughnut.git (fetch)
origin  https://github.com/nerds-odd-e/doughnut.git (push)
```

2. Change the remote URL to use SSH:
```bash
git remote set-url origin git@github.com:nerds-odd-e/doughnut.git
```

3. Verify the change:
```bash
git remote -v
```

It should now show:
```
origin  git@github.com:nerds-odd-e/doughnut.git (fetch)
origin  git@github.com:nerds-odd-e/doughnut.git (push)
```

4. Make sure you have SSH keys set up:

Check if you have existing SSH keys:
```bash
ls -al ~/.ssh
```

If you don't have keys (id_rsa and id_rsa.pub), generate them (replace `your_email@example.com` with your own github account's email address):
```bash
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"
```

Add your SSH key to the ssh-agent:
```bash
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_rsa
```

Add your SSH public key to your GitHub account (if not already done):
```bash
cat ~/.ssh/id_rsa.pub
```
Then copy the output and add it in GitHub under Settings > SSH and GPG keys


Test your SSH connection:
```bash
ssh -T git@github.com
```

That's it! Your repository is now set up to use SSH instead of HTTPS for Git operations.