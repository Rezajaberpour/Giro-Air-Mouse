package com.example.util

object ServerScripts {

  val PYTHON_SCRIPT: String = """
# =====================================================================
# GyroMouse Windows 10 Server (Port 8620 - Giro Air Mouse Compatible)
# Run with: python windows_server.py
# =====================================================================
import socket
import ctypes
import sys
import threading
import time

user32 = ctypes.windll.user32

# Windows mouse_event constants
MOUSEEVENTF_MOVE = 0x0001
MOUSEEVENTF_LEFTDOWN = 0x0002
MOUSEEVENTF_LEFTUP = 0x0004
MOUSEEVENTF_RIGHTDOWN = 0x0008
MOUSEEVENTF_RIGHTUP = 0x0010
MOUSEEVENTF_MIDDLEDOWN = 0x0020
MOUSEEVENTF_MIDDLEUP = 0x0040
MOUSEEVENTF_WHEEL = 0x0800

PORT = 8620

def get_local_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"

def main():
    local_ip = get_local_ip()
    print("=====================================================")
    print("   🎮 Giro Air Mouse Server - Windows 10 Active      ")
    print("=====================================================")
    print("[*] Address: http://" + local_ip + ":" + str(PORT))
    print("[*] Listening on Port: " + str(PORT))
    print("[*] Enter '" + local_ip + "' or '" + local_ip + ":" + str(PORT) + "' in app!")
    print("=====================================================")
    print("[*] Ready! Move your phone and press Volume Up to click.")

    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind(("0.0.0.0", PORT))

    # Sub-pixel accumulator for ultra-smooth pointer tracking
    acc_x = 0.0
    acc_y = 0.0

    while True:
        try:
            data, addr = sock.recvfrom(512)
            msg = data.decode("utf-8", errors="ignore").strip()

            if msg.startswith("PING"):
                token = msg.split(":", 1)[1] if ":" in msg else ""
                sock.sendto(("PONG:" + token).encode("utf-8"), addr)
                continue

            if msg == "GYROMOUSE_DISCOVER" or msg == "MOUSE_AIR_DISCOVERY":
                sock.sendto(("GYROMOUSE_SERVER:" + str(PORT)).encode("utf-8"), addr)
                print("[+] Discovered by phone at " + str(addr[0]))
                continue

            parts = msg.split(":")
            cmd = parts[0]

            if cmd == "M" and len(parts) >= 3:
                dx = float(parts[1])
                dy = float(parts[2])
                acc_x += dx
                acc_y += dy
                step_x = int(acc_x)
                step_y = int(acc_y)
                if step_x != 0 or step_y != 0:
                    acc_x -= step_x
                    acc_y -= step_y
                    user32.mouse_event(MOUSEEVENTF_MOVE, step_x, step_y, 0, 0)

            elif cmd == "MD": # Mouse Down (Hold)
                btn = parts[1] if len(parts) > 1 else "L"
                if btn == "L":
                    user32.mouse_event(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0)
                elif btn == "R":
                    user32.mouse_event(MOUSEEVENTF_RIGHTDOWN, 0, 0, 0, 0)
                elif btn == "M":
                    user32.mouse_event(MOUSEEVENTF_MIDDLEDOWN, 0, 0, 0, 0)

            elif cmd == "MU": # Mouse Up (Release)
                btn = parts[1] if len(parts) > 1 else "L"
                if btn == "L":
                    user32.mouse_event(MOUSEEVENTF_LEFTUP, 0, 0, 0, 0)
                elif btn == "R":
                    user32.mouse_event(MOUSEEVENTF_RIGHTUP, 0, 0, 0, 0)
                elif btn == "M":
                    user32.mouse_event(MOUSEEVENTF_MIDDLEUP, 0, 0, 0, 0)

            elif cmd == "C": # Mouse Click
                btn = parts[1] if len(parts) > 1 else "L"
                if btn == "L":
                    user32.mouse_event(MOUSEEVENTF_LEFTDOWN | MOUSEEVENTF_LEFTUP, 0, 0, 0, 0)
                elif btn == "R":
                    user32.mouse_event(MOUSEEVENTF_RIGHTDOWN | MOUSEEVENTF_RIGHTUP, 0, 0, 0, 0)
                elif btn == "M":
                    user32.mouse_event(MOUSEEVENTF_MIDDLEDOWN | MOUSEEVENTF_MIDDLEUP, 0, 0, 0, 0)

            elif cmd == "S" and len(parts) >= 2: # Scroll Wheel
                scroll_amount = int(float(parts[1]) * 120)
                user32.mouse_event(MOUSEEVENTF_WHEEL, 0, 0, scroll_amount, 0)

        except KeyboardInterrupt:
            print("\nExiting server.")
            break
        except Exception:
            pass

if __name__ == "__main__":
    main()
""".trimIndent()

  val POWERSHELL_SCRIPT: String = """
# =====================================================================
# GyroMouse Windows 10 PowerShell Server (Port 8620 - Giro Air Mouse Compatible)
# Run in PowerShell: powershell -ExecutionPolicy Bypass -File server.ps1
# =====================================================================
Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
public class WinMouse {
    [DllImport("user32.dll")]
    public static extern void mouse_event(int dwFlags, int dx, int dy, int dwData, int dwExtraInfo);
}
"@

@D@PORT = 8620
@D@udpClient = New-Object System.Net.Sockets.UdpClient(@D@PORT)
@D@endpoint = New-Object System.Net.IPEndPoint([System.Net.IPAddress]::Any, 0)

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "   🎮 Giro Air Mouse Server Windows 10 (PS)          " -ForegroundColor Green
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "[*] Listening on UDP Port: @D@PORT" -ForegroundColor Yellow
Write-Host "[*] Move phone & press Volume Up to click!" -ForegroundColor White

try {
    while (@D@true) {
        @D@bytes = @D@udpClient.Receive([ref]@D@endpoint)
        @D@msg = [System.Text.Encoding]::UTF8.GetString(@D@bytes).Trim()

        if (@D@msg.StartsWith("PING")) {
            @D@resp = [System.Text.Encoding]::UTF8.GetBytes("PONG")
            @D@null = @D@udpClient.Send(@D@resp, @D@resp.Length, @D@endpoint)
            continue
        }
        if (@D@msg -eq "GYROMOUSE_DISCOVER" -or @D@msg -eq "MOUSE_AIR_DISCOVERY") {
            @D@resp = [System.Text.Encoding]::UTF8.GetBytes("GYROMOUSE_SERVER:@D@PORT")
            @D@null = @D@udpClient.Send(@D@resp, @D@resp.Length, @D@endpoint)
            continue
        }

        @D@p = @D@msg.Split(":")
        switch (@D@p[0]) {
            "M" {
                @D@dx = [int][Math]::Round([float]@D@p[1])
                @D@dy = [int][Math]::Round([float]@D@p[2])
                [WinMouse]::mouse_event(0x0001, @D@dx, @D@dy, 0, 0)
            }
            "MD" {
                if (@D@p[1] -eq "L") { [WinMouse]::mouse_event(0x0002, 0, 0, 0, 0) }
                elseif (@D@p[1] -eq "R") { [WinMouse]::mouse_event(0x0008, 0, 0, 0, 0) }
            }
            "MU" {
                if (@D@p[1] -eq "L") { [WinMouse]::mouse_event(0x0004, 0, 0, 0, 0) }
                elseif (@D@p[1] -eq "R") { [WinMouse]::mouse_event(0x0010, 0, 0, 0, 0) }
            }
            "C" {
                if (@D@p[1] -eq "L") { [WinMouse]::mouse_event(0x0006, 0, 0, 0, 0) }
                elseif (@D@p[1] -eq "R") { [WinMouse]::mouse_event(0x0018, 0, 0, 0, 0) }
            }
            "S" {
                @D@amt = [int]([float]@D@p[1] * 120)
                [WinMouse]::mouse_event(0x0800, 0, 0, @D@amt, 0)
            }
        }
    }
} finally {
    @D@udpClient.Close()
}
""".trimIndent().replace("@D@", "$")
}
