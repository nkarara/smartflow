# =====================================================================
# SmartFlow — Test E2E des fonctionnalités via l'API REST
# Usage : powershell -File scripts/e2e-smoke.ps1  (backend lancé sur :8082)
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

function Step($title) {
    Write-Host ""
    Write-Host "======== $title ========" -ForegroundColor Cyan
}

# ------------------------------------------------- 1. Santé
Step "1. SANTE (actuator)"
$health = Invoke-RestMethod -Uri "$Base/actuator/health" -TimeoutSec 5
Write-Host "status = $($health.status)"

# ------------------------------------------------- 2. Authentification
Step "2. AUTHENTIFICATION (login x3 + login invalide)"
$client = Call "POST" "/auth/login" $null @{ email = "client@smartflow.fr"; password = "Client@123" }
$admin  = Call "POST" "/auth/login" $null @{ email = "admin@smartflow.fr";  password = "Admin@123" }
$tech   = Call "POST" "/auth/login" $null @{ email = "tech@smartflow.fr";   password = "Tech@123" }
Write-Host "client = $($client.user.email) / role=$($client.user.role) / token=$($client.accessToken.Substring(0,22))..."
Write-Host "admin  = $($admin.user.email) / role=$($admin.user.role)"
Write-Host "tech   = $($tech.user.email) / role=$($tech.user.role)"
$badLogin = try {
    Call "POST" "/auth/login" $null @{ email = "client@smartflow.fr"; password = "Wrong!" }
    "PAS D'ERREUR (BUG)"
} catch {
    "mauvais mot de passe -> HTTP $($_.Exception.Response.StatusCode.value__) (attendu 401 suite au filtre) -> $($_.Exception.Message.Split([char]10)[0])"
}
Write-Host "login invalide : $badLogin"

# ------------------------------------------------- 3. IA
Step "3. INTELLIGENCE ARTIFICIELLE - classification de la demande"
$ai = Call "POST" "/ai/analyze" $client.accessToken @{
    title = "Imprimante en panne"
    description = "L'imprimante du bureau affiche une erreur et ne veut plus imprimer"
}
Write-Host "categorie=$($ai.category) | type=$($ai.type) | priorite=$($ai.priority) | temps=$($ai.estimatedTimeMinutes)min | analyseur=$($ai.analyzer)"
Write-Host "probleme probable: $($ai.probableProblem)"

# ------------------------------------------------- 4. Création
Step "4. CREATION D'UNE DEMANDE (client)"
$cats = Call "GET" "/categories" $client.accessToken $null
$catMateriel = $cats | Where-Object { $_.name -like "*Matériel*" } | Select-Object -First 1
$created = Call "POST" "/interventions" $client.accessToken @{
    title = "Ordinateur qui ne demarre plus"
    description = "Ecran noir au demarrage depuis ce matin, voyant disque rouge."
    categoryId = $catMateriel.id
    priority = "URGENT"
    location = "Paris"
    estimatedTimeMinutes = 90
}
$interventionId = $created.id
Write-Host "id=$interventionId | statut=$($created.status) | client=$($created.clientName) | cat=$($created.categoryName)"

# ------------------------------------------------- 5. Commentaire
Step "5. COMMENTAIRE (client)"
$comment = Call "POST" "/interventions/$interventionId/comments" $client.accessToken @{ content = "Pouvez-vous intervenir ce jour ?" }
Write-Host "commentaire de $($comment.authorName) : $($comment.content)"

# ------------------------------------------------- 6. Pièce jointe
Step "6. PIECE JOINTE (upload multipart via curl)"
$tmp = Join-Path $env:TEMP "smartflow-doc.txt"
[System.IO.File]::WriteAllText($tmp, "Photo du poste en panne - intervention $interventionId")
$uploadJson = & curl.exe -s -X POST -H "Authorization: Bearer $($client.accessToken)" `
    -F "file=@$tmp;type=text/plain" "$api/interventions/$interventionId/attachments"
$attachment = $uploadJson | ConvertFrom-Json
Write-Host "fichier : $($attachment.fileName) ($([math]::Round($attachment.size/1KB,1)) Ko) par $($attachment.uploadedByName)"

# PART2
# ------------------------------------------------- 7. Autorisations
Step "7. SECURITE / AUTORISATIONS"
$forbidden = try {
    Call "GET" "/users" $client.accessToken $null
    "PAS D'ERREUR (BUG)"
} catch {
    "client -> /api/users : HTTP $($_.Exception.Response.StatusCode.value__) (attendu 403)"
}
Write-Host $forbidden
$forbidden2 = try {
    Call "GET" "/users" $tech.accessToken $null
    "PAS D'ERREUR (BUG)"
} catch {
    "technicien -> /api/users : HTTP $($_.Exception.Response.StatusCode.value__) (attendu 403)"
}
Write-Host $forbidden2

# ------------------------------------------------- 8. Dashboard admin
Step "8. TABLEAU DE BORD ADMIN"
$dash = Call "GET" "/dashboard/admin" $admin.accessToken $null
Write-Host "interventions=$($dash.totalInterventions) | en cours=$($dash.inProgress) | urgentes=$($dash.urgent) | cloturees=$($dash.closed) | taux res.=$([math]::Round($dash.resolutionRate,1))%"
Write-Host "par mois: $(($dash.byMonth | ForEach-Object { "$($_.month):$($_.count)" }) -join ', ')"
Write-Host "tech top: $(($dash.technicianPerformance | Select-Object -First 2 | ForEach-Object { "$($_.technicianName) (terminees=$($_.completed))" }) -join ' | ')"

# ------------------------------------------------- 9. Suggestions + affectation
Step "9. SUGGESTIONS IA + AFFECTATION (admin)"
$suggestions = Call "GET" "/interventions/$interventionId/assign/suggestions" $admin.accessToken $null
Write-Host "suggestions: $(($suggestions | Select-Object -First 3 | ForEach-Object { "$($_.technicianName) (score=$($_.score))" }) -join '  |  ')"
$techs = Call "GET" "/technicians" $admin.accessToken $null
$techId = ($techs | Where-Object { $_.email -eq "tech@smartflow.fr" } | Select-Object -First 1).id
$assigned = Call "PUT" "/interventions/$interventionId/assign" $admin.accessToken @{ technicianId = $techId }
Write-Host "statut=$($assigned.status) | technicien=$($assigned.technicianName)"

# ------------------------------------------------- 10. Cycle de vie (technicien)
Step "10. CYCLE DE VIE - technicien (accepter -> demarrer -> resoudre -> compte rendu)"
$s = Call "PUT" "/interventions/$interventionId/status" $tech.accessToken @{ newStatus = "ACCEPTED" }
Write-Host "-> $($s.status)"
$s = Call "PUT" "/interventions/$interventionId/status" $tech.accessToken @{ newStatus = "IN_PROGRESS" }
Write-Host "-> $($s.status)"
$s = Call "PUT" "/interventions/$interventionId/status" $tech.accessToken @{ newStatus = "RESOLVED" }
Write-Host "-> $($s.status)"
$s = Call "PUT" "/interventions/$interventionId/account" $tech.accessToken @{
    actualTimeMinutes = 95
    report = "Carte reseau remplacee puis test de connexion concluant."
}
Write-Host "compte rendu enregistre (temps=$($s.actualTimeMinutes) min)"

# ------------------------------------------------- 11. Évaluation + clôture
Step "11. EVALUATION (client) + CLOTURE (admin)"
$rating = Call "POST" "/interventions/$interventionId/rating" $client.accessToken @{ score = 5; comment = "Intervention rapide et efficace." }
Write-Host "note = $($rating.score)/5"
$closed = Call "PUT" "/interventions/$interventionId/status" $admin.accessToken @{ newStatus = "CLOSED" }
Write-Host "statut final = $($closed.status) | cloturee le $($closed.closedAt)"

# ------------------------------------------------- 12. Notifications
Step "12. NOTIFICATIONS (technicien)"
$notifs = Call "GET" "/notifications?unreadOnly=true" $tech.accessToken $null
Write-Host "technicien : $($notifs.Count) notification(s) non lue(s) -> $($notifs[0].message)"

# ------------------------------------------------- 13. Historique
Step "13. HISTORIQUE DE L'INTERVENTION"
$history = Call "GET" "/interventions/$interventionId/history" $admin.accessToken $null
Write-Host ($history | ForEach-Object { "  $($_.fromStatus) -> $($_.toStatus)  ($($_.changedByName))" })

# ------------------------------------------------- 14. Stats tech + résumé IA
Step "14. STATISTIQUES TECHNICIEN et RESUME IA"
$stats = Call "GET" "/dashboard/technician" $tech.accessToken $null
Write-Host "mes interventions=$($stats.myInterventions) | en cours=$($stats.inProgress) | terminees=$($stats.completed) | temps moyen=$($stats.avgInterventionHours)h"
$summary = Call "POST" "/ai/summarize" $tech.accessToken @{
    reportText = "Carte reseau remplacee. Test de connexion concluant. Utilisateur informe."
    title = "Ordinateur qui ne demarre plus"
}
Write-Host "resume IA : $($summary.summary)"

Write-Host ""
Write-Host "=== FIN : toutes les fonctionnalites ont ete testees ===" -ForegroundColor Green