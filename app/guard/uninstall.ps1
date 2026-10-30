#requires -version 5
<#
.SYNOPSIS
  卸载「桌面守卫」，恢复原状。
.DESCRIPTION
  奇异果桌面（com.tvfengmi.launcher）全程没禁用过，所以不需要恢复它。
  这里只把守卫从无障碍服务列表里摘掉，再卸载。
.EXAMPLE
  .\uninstall.ps1 -Device 192.168.1.60:5555
#>
param(
    [Parameter(Mandatory = $true)][string]$Device,
    [string]$Adb = ""
)

$ErrorActionPreference = "Stop"

if (-not $Adb) {
    $c = Get-Command adb -ErrorAction SilentlyContinue
    if ($c) { $Adb = $c.Source }
    else {
        foreach ($p in @("$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe", "C:\platform-tools\adb.exe")) {
            if (Test-Path $p) { $Adb = $p; break }
        }
    }
}
if (-not $Adb) { throw "找不到 adb，用 -Adb 指定路径" }

Write-Output "== 1/2 从无障碍服务列表移除守卫（保留其它服务）=="
$cur = ((& $Adb -s $Device shell "settings get secure enabled_accessibility_services" 2>&1) -join "").Trim()
$rest = @($cur -split ':' | Where-Object { $_ -and ($_ -notmatch 'dsh\.peppa\.guard') })
if ($rest.Count -gt 0) {
    $new = $rest -join ':'
    & $Adb -s $Device shell "settings put secure enabled_accessibility_services '$new'" 2>&1 | Out-Null
    Write-Output "  保留：$new"
} else {
    & $Adb -s $Device shell "settings put secure accessibility_enabled 0" 2>&1 | Out-Null
    Write-Output "  没有其它无障碍服务，已关闭无障碍总开关"
}

Write-Output "== 2/2 卸载 com.dsh.peppa.guard =="
& $Adb -s $Device uninstall com.dsh.peppa.guard 2>&1

Write-Output ""
Write-Output "完成。Home 键回到原来由其它无障碍服务抢占的行为（会先闪一下奇异果桌面）。"
