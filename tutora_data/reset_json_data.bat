@echo off
REM ============================================================
REM  reset_json_data.bat
REM  Ripristina i file JSON allo stato seed pulito.
REM  Eseguire PRIMA di ogni sessione di test in modalita JSON.
REM ============================================================

set DATA=%~dp0
set SEED=%DATA%_seed\

echo Ripristino file JSON in corso...

copy /Y "%SEED%users.json"              "%DATA%users.json"
copy /Y "%SEED%tutorExpertises.json"    "%DATA%tutorExpertises.json"
copy /Y "%SEED%categories.json"         "%DATA%categories.json"
copy /Y "%SEED%bookings.json"           "%DATA%bookings.json"
copy /Y "%SEED%lessons.json"            "%DATA%lessons.json"
copy /Y "%SEED%notifications.json"      "%DATA%notifications.json"
copy /Y "%SEED%reviews.json"            "%DATA%reviews.json"
copy /Y "%SEED%tutor_applications.json" "%DATA%tutor_applications.json"
copy /Y "%SEED%application_items.json"  "%DATA%application_items.json"
copy /Y "%SEED%documents.json"          "%DATA%documents.json"
copy /Y "%SEED%avatars.properties"      "%DATA%avatars.properties"

echo.
echo Reset completato. Dati ripristinati allo stato seed.
pause
