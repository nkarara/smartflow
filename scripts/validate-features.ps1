# =====================================================================
# SmartFlow — Validation des NOUVELLES fonctionnalités (paramètres + dashboard catégorie)
# Usage : powershell -File scripts/validate-features.ps1   (backend sur :8082)
# =====================================================================
param(
    [string]$Base = "http://localhost:8082"
)

$ErrorActionPreference = "Stop"
$api = "$Base/api"

function ToJson($obj) { $obj | ConvertTo-Json -Depth 8 }

function Call($method, $uri, $token, $payload) {
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    $params = @{ Method = $method; Uri = "$api$uri"; Headers = $headers }
    if ($null -ne $payload) {
        $params["ContentType"] = "application/json; charset=utf-8"
        $params["Body"] = [System.Text.Encoding]::UTF8.GetBytes((ToJson($payload)))
    }
    return Invoke-RestMethod @params
}

Write-Host "===== 1. CONNEXION ADMIN =====" -ForegroundColor Cyan
$admin = Call "POST" "/auth/login" $null @{ email = "admin@smartflow.fr"; password = "Admin@123" }
$client = Call "POST" "/auth/login" $null @{ email = "client@smartflow.fr"; password = "Client@123" }
Write-Host "admin connecté : $($admin.user.email)"

Write-Host ""
Write-Host "===== 2. LISTE DES PARAMETRES (admin) =====" -ForegroundColor Cyan
$settings = Call "GET" "/settings" $admin.accessToken $null
Write-Host "nombre de parametres : $($settings.Count)"
$settings | ForEach-Object { Write-Host "  - $($_.key) = $($_.value) [$($_.category)]" }

Write-Host ""
Write-Host "===== 3. MISE A JOUR D'UN PARAMETRE (admin) =====" -ForegroundColor Cyan
$updated = Call "PUT" "/settings/sla.defaultResponseHours" $admin.accessToken @{ value = "8" }
Write-Host "sla.defaultResponseHours = $($updated.value) (attendue : 8)"

Write-Host ""
Write-Host "===== 4. SECURITE : UN CLIENT SUR /api/settings =====" -ForegroundColor Cyan
$forbidden = try {
    Call "GET" "/settings" $client.accessToken $null
    "PAS D'ERREUR (BUG)"
} catch {
    "client -> /api/settings : HTTP $($_.Exception.Response.StatusCode.value__) (attendu 403)"
}
Write-Host $forbidden

Write-Host ""
Write-Host "===== 5. DASHBOARD ADMIN : byCategory =====" -ForegroundColor Cyan
$dash = Call "GET" "/dashboard/admin" $admin.accessToken $null
Write-Host "interventions par categorie : $(($dash.byCategory | ForEach-Object { "$($_.name):$($_.count)" }) -join ', ')"
Write-Host "interventions par mois       : $(($dash.byMonth | ForEach-Object { "$($_.month):$($_.count)" }) -join ', ')"

Write-Host ""
Write-Host "===== 6. VALIDATION ENTREE VIDE => 400 (admin) =====" -ForegroundColor Cyan
$bad = try {
    Call "PUT" "/settings/support.email" $admin.accessToken @{ value = "   " }
    "PAS D'ERREUR (BUG)"
} catch {
    "valeur vide -> HTTP $($_.Exception.Response.StatusCode.value__) (attendu 400)"
}
Write-Host $bad

Write-Host ""
Write-Host "===== FIN : NOUVELLES FONCTIONNALITES VALIDEES =====" -ForegroundColor Green