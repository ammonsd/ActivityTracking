Start-Process -FilePath "wsl" -ArgumentList "-u", "root", "journalctl", "-u", "jenkins", "-f" -WindowStyle Hidden
