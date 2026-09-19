# Vérification finale : nouvelle fonctionnalité servie via le frontend (proxy 5173 → 8082)
$loginBody = '{"email":"admin@smartflow.fr","password":"Admin@123"}'
$login = Invoke-RestMethod -Method Post -Uri 'http://localhost:5173/api/auth/login' -ContentType 'application/json' -Body ([System.Text.Encoding]::UTF8.GetBytes($loginBody))
$headers = @{ Authorization = ("Bearer " + $login.accessToken) }
$settings = Invoke-RestMethod -Uri 'http://localhost:5173/api/settings' -Headers $headers
Write-Output ("proxy 5173 -> /api/settings : " + $settings.Count + " parametres (ok)")