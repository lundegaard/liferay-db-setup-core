@echo off

:: Check the input parameters

if "%~1"=="" (
    call :echoUsage %0
    goto :eof
)

if "%~2"=="" (
    call :echoUsage %0
    goto :eof
)

:: Variables

set RELEASE_VERSION=%1
set NEXT_DEV_VERSION=%2

@echo on

:: Actualize local develop & master branches
git checkout master
git merge --ff-only origin/master
if %ERRORLEVEL% NEQ 0 (EXIT /B 1)
git checkout develop
git merge --ff-only origin/develop
if %ERRORLEVEL% NEQ 0 (EXIT /B 1)

:: Build develop
mvn clean package
if %ERRORLEVEL% NEQ 0 (EXIT /B 1)

:: Create release branch
git branch release/%RELEASE_VERSION% develop
if %ERRORLEVEL% NEQ 0 (EXIT /B 1)

:: Maven release version
git checkout release/%RELEASE_VERSION%
mvn versions:set -DnewVersion="%RELEASE_VERSION%" -DgenerateBackupPoms=false
if %ERRORLEVEL% NEQ 0 (EXIT /B 1)
git add "*pom.xml"
git commit -m "release/%RELEASE_VERSION% Maven project version set to %RELEASE_VERSION%"

:: Finish the release branch
git checkout master
git merge -m "Merge branch 'release/%RELEASE_VERSION%' into master" --no-ff release/%RELEASE_VERSION%
if %ERRORLEVEL% NEQ 0 (EXIT /B 1)
git branch -d release/%RELEASE_VERSION%
if %ERRORLEVEL% NEQ 0 (EXIT /B 1)

:: Build new production artifacts
mvn clean deploy -P release
if %ERRORLEVEL% NEQ 0 (EXIT /B 1)

:: Tag the new relase version
git tag -m "Version %RELEASE_VERSION%" -a v%RELEASE_VERSION%
if %ERRORLEVEL% NEQ 0 (EXIT /B 1)

:: Merge master back to develop branch
git checkout develop
git merge -m "Merge branch 'master' with version '%RELEASE_VERSION%' into develop" --no-ff master
if %ERRORLEVEL% NEQ 0 (EXIT /B 1)

:: Maven next development version
mvn versions:set -DnewVersion="%NEXT_DEV_VERSION%" -DgenerateBackupPoms=false
if %ERRORLEVEL% NEQ 0 (EXIT /B 1)
git add "*pom.xml"
git commit -m "release/%RELEASE_VERSION% Development continues on version %NEXT_DEV_VERSION%"

:: Push the changes to the 'origin' repo
git push --tags origin master develop

EXIT /B %ERRORLEVEL%

:: Functions

:echoUsage
    echo Usage:
    echo.
    echo %~0 ^<release-version^> ^<next-dev-version^>
    echo.
    echo - release-version - The version which should be tagged (e.g. '1.0.0')
    echo - next-dev-version - Next version which will be used for further development (e.g. '1.1.0-SNAPSHOT')
    echo.
EXIT /B 0
