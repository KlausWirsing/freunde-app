Add-Type -AssemblyName System.Drawing

$bg = [System.Drawing.Color]::FromArgb(255, 46, 89, 69)     # #2e5945
$white = [System.Drawing.Color]::White

function New-RoundedRectPath($x, $y, $w, $h, $r) {
  $path = New-Object System.Drawing.Drawing2D.GraphicsPath
  $d = $r * 2
  $path.AddArc($x, $y, $d, $d, 180, 90)
  $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
  $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
  $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
  $path.CloseFigure()
  return $path
}

function Draw-Person($g, $brush, $cx, $headY, $headR, $bodyTop, $bodyW, $bodyH) {
  $g.FillEllipse($brush, $cx - $headR, $headY - $headR, $headR * 2, $headR * 2)
  $bodyPath = New-RoundedRectPath ($cx - $bodyW / 2.0) $bodyTop $bodyW $bodyH ($bodyW * 0.4)
  $g.FillPath($brush, $bodyPath)
}

function New-Icon($size, $outPath, $maskable) {
  $bmp = New-Object System.Drawing.Bitmap $size, $size
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $g.Clear([System.Drawing.Color]::Transparent)

  $bgBrush = New-Object System.Drawing.SolidBrush $bg
  if ($maskable) {
    $g.FillRectangle($bgBrush, 0, 0, $size, $size)
    $scale = 0.60
  } else {
    $corner = [double]$size * 0.22
    $path = New-RoundedRectPath 0 0 $size $size $corner
    $g.FillPath($bgBrush, $path)
    $scale = 0.78
  }

  $cx = $size / 2.0
  $cy = $size / 2.0
  $unit = $size * $scale

  $whiteBrush = New-Object System.Drawing.SolidBrush $white

  # Hintere Person (versetzt, halbtransparent wirkend durch etwas kleineren, helleren Ton)
  $backBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(190, 255, 255, 255))
  Draw-Person $g $backBrush ($cx + $unit * 0.20) ($cy - $unit * 0.16) ($unit * 0.15) ($cy + $unit * 0.02) ($unit * 0.38) ($unit * 0.34)

  # Vordere Person
  Draw-Person $g $whiteBrush ($cx - $unit * 0.14) ($cy - $unit * 0.20) ($unit * 0.18) ($cy + $unit * 0.02) ($unit * 0.46) ($unit * 0.40)

  $g.Dispose()
  $bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
  $bmp.Dispose()
}

$iconsDir = Join-Path $PSScriptRoot "icons"
if (-not (Test-Path $iconsDir)) { New-Item -ItemType Directory -Path $iconsDir | Out-Null }

New-Icon 192 (Join-Path $iconsDir "icon-192.png") $false
New-Icon 512 (Join-Path $iconsDir "icon-512.png") $false
New-Icon 192 (Join-Path $iconsDir "icon-maskable-192.png") $true
New-Icon 512 (Join-Path $iconsDir "icon-maskable-512.png") $true

Write-Output "Icons erstellt."
