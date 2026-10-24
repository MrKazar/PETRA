<#
.SYNOPSIS
    Publie une version approuvée de PETRA : APK signé + release GitHub (canal de mise à jour de l'app).

.DESCRIPTION
    1. Vérifie que la clé de signature (keystore.properties) est présente et que git est propre.
    2. Passe la version à -Version dans gradle.properties (versionCode + 1).
    3. Construit l'APK de release signé et le copie dans releases\PETRA-<version>.apk.
    4. Commit « Version <version> », tag v<version>, push vers GitHub.
    5. Crée la release GitHub v<version> avec l'APK en pièce jointe.
       Les téléphones avec PETRA proposent alors la mise à jour au lancement suivant.

    Authentification GitHub : la CLI « gh » si elle est installée et connectée,
    sinon un jeton dans la variable d'environnement GITHUB_TOKEN (droits « Contents : read and write »).

.EXAMPLE
    .\scripts\publish-release.ps1 -Version 1.2.0 -Notes "Planning sur deux semaines"
.EXAMPLE
    .\scripts\publish-release.ps1 -Version 1.2.0 -DryRun   # construit l'APK sans rien publier
#>
param(
    [Parameter(Mandatory = $true)][string]$Version,
    [string]$Notes = "",
    [switch]$DryRun
)

# « Continue » : Windows PowerShell 5.1 transforme les messages d'avertissement des outils (gradle, git)
# en erreurs fatales sous « Stop ». On vérifie plutôt le code de sortie de chaque commande.
$ErrorActionPreference = "Continue"
function Check($what) { if ($LASTEXITCODE -ne 0) { Fail "$what a échoué (code $LASTEXITCODE)." } }
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Fail($message) { Write-Host "ERREUR : $message" -ForegroundColor Red; exit 1 }

if ($Version -notmatch '^\d+\.\d+\.\d+$') { Fail "la version doit être de la forme 1.2.3 (reçu : $Version)." }
if (-not (Test-Path "keystore.properties")) {
    Fail "keystore.properties introuvable. Sans la clé de signature, le téléphone refusera la mise à jour."
}

$propsFile = Join-Path $root "gradle.properties"
$props = Get-Content $propsFile -Encoding UTF8
$repo = (($props | Where-Object { $_ -match '^petra\.updateRepo=' }) -replace '^petra\.updateRepo=', '').Trim()
$currentName = (($props | Where-Object { $_ -match '^petra\.versionName=' }) -replace '^petra\.versionName=', '').Trim()
$currentCode = [int](($props | Where-Object { $_ -match '^petra\.versionCode=' }) -replace '^petra\.versionCode=', '')

if ([version]$Version -le [version]$currentName) { Fail "la version $Version doit être supérieure à la version actuelle $currentName." }

if (-not $DryRun) {
    if (git status --porcelain) { Fail "des modifications ne sont pas commitées. Commite ou annule-les avant de publier." }
    if (-not (git remote)) { Fail "aucun dépôt distant git. Ajoute-le : git remote add origin https://github.com/$repo.git" }
}

$newCode = $currentCode + 1
Write-Host "PETRA $currentName ($currentCode)  ->  $Version ($newCode)   dépôt : $repo" -ForegroundColor Cyan

$props = $props -replace '^petra\.versionName=.*', "petra.versionName=$Version" -replace '^petra\.versionCode=.*', "petra.versionCode=$newCode"
[System.IO.File]::WriteAllLines($propsFile, $props, (New-Object System.Text.UTF8Encoding($false)))

Write-Host "Construction de l'APK signé..." -ForegroundColor Cyan
& .\gradlew.bat :app:assembleRelease --console=plain -q
if ($LASTEXITCODE -ne 0) {
    git checkout -- gradle.properties
    Fail "la construction a échoué ; gradle.properties est remis comme avant."
}

$apkSource = Join-Path $root "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $apkSource)) { Fail "APK introuvable : $apkSource" }
New-Item -ItemType Directory -Force (Join-Path $root "releases") | Out-Null
$apk = Join-Path $root "releases\PETRA-$Version.apk"
Copy-Item $apkSource $apk -Force
Write-Host "APK prêt : $apk" -ForegroundColor Green

if ($DryRun) {
    git checkout -- gradle.properties
    Write-Host "Essai à blanc : rien n'a été commité ni publié (gradle.properties remis comme avant)." -ForegroundColor Yellow
    exit 0
}

$tag = "v$Version"
git add gradle.properties
git commit -q -m "Version $Version"; Check "git commit"
git tag -a $tag -m "PETRA $Version"; Check "git tag"
git push -q origin HEAD; Check "git push"
git push -q origin $tag; Check "git push du tag"

if (-not $Notes) { $Notes = "PETRA $Version" }

if (Get-Command gh -ErrorAction SilentlyContinue) {
    gh release create $tag $apk --repo $repo --title "PETRA $Version" --notes $Notes
    if ($LASTEXITCODE -ne 0) { Fail "la création de la release avec gh a échoué." }
} else {
    $token = $env:GITHUB_TOKEN
    if (-not $token) { Fail "ni « gh » ni GITHUB_TOKEN : la version est taguée et poussée, mais la release n'est pas créée. Crée-la à la main sur https://github.com/$repo/releases/new?tag=$tag avec $apk." }
    $headers = @{ Authorization = "Bearer $token"; Accept = "application/vnd.github+json"; "User-Agent" = "PETRA-release" }
    $body = @{ tag_name = $tag; name = "PETRA $Version"; body = $Notes; draft = $false; prerelease = $false } | ConvertTo-Json
    $ErrorActionPreference = "Stop"
    $release = Invoke-RestMethod -Method Post -Uri "https://api.github.com/repos/$repo/releases" -Headers $headers -Body ([System.Text.Encoding]::UTF8.GetBytes($body)) -ContentType "application/json; charset=utf-8"
    $uploadUrl = "https://uploads.github.com/repos/$repo/releases/$($release.id)/assets?name=PETRA-$Version.apk"
    Invoke-RestMethod -Method Post -Uri $uploadUrl -Headers $headers -InFile $apk -ContentType "application/vnd.android.package-archive" | Out-Null
}

Write-Host "Version $Version publiée. PETRA la proposera au prochain lancement sur le téléphone." -ForegroundColor Green
