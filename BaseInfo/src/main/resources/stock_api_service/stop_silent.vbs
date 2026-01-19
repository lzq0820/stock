Set WshShell = CreateObject("WScript.Shell")
WshShell.Run chr(34) & "stop_api_silent.bat" & chr(34), 0
Set WshShell = Nothing