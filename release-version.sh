#!/bin/sh

# Check the input params
if [ $# -ne 2 ]; then
	echo "Usage:"
	echo ""
	echo "$0 <release-version> <next-dev-version>"
	echo ""
	echo "- release-version - The version which should be tagged (e.g. '1.0.0')"
	echo "- next-dev-version - Next version which will be used for further development (e.g. '1.1.0-SNAPSHOT')"
	echo ""
	exit
fi

# Stop the script on first error, output commands before executing
set -e -x

# Build develop
git checkout develop
mvn clean package

# Create release branch
git branch release/$1 develop

# Maven release version
git checkout release/$1
mvn versions:set -DnewVersion="$1" -DgenerateBackupPoms=false
git add "*pom.xml"
git commit -m "release/$1 Maven project version set to $1"

# Build and analyze with sonar
mvn clean jacoco:prepare-agent install sonar:sonar -P lundegaard-sonar

# Finish the release branch
git checkout master
git merge -m "Merge branch 'release/$1' into master" --no-ff release/$1
git branch -d release/$1

# Build new production artifacts
mvn clean deploy -P release

# Tag the new relase version
git tag -m "Version $1" -a v$1

# Merge master back to develop branch
git checkout develop
git merge -m "Merge branch 'master' with version '$1' into develop" --no-ff master

# Maven next development version
mvn versions:set -DnewVersion="$2" -DgenerateBackupPoms=false
git add "*pom.xml"
git commit -m "release/$1 Development continues on version $2"

# Push the changes to the 'origin' repo
git push --tags origin master develop
