Set objShell = CreateObject("WScript.Shell")
objShell.Run "wsl -u root journalctl -u jenkins -f", 0, False
