#!/bin/sh

set -e

app="fuel-log"
version=$(git describe --tags | tr '-' '.' | sed -e 's/^v//')

rootProjectDir=$( cd $(dirname $0) && cd ../.. && pwd )

$rootProjectDir/gradlew :assemble
tmpdir=$(mktemp -d)
trap 'rm -rf "$tmpdir"' EXIT

unzip -d $tmpdir $rootProjectDir/build/distributions/$app-$version.zip
mv $tmpdir/$app-$version $tmpdir/app
unzip -d $tmpdir/app/content $tmpdir/app/lib/$app-$version.jar

cp $(dirname $0)/Dockerfile $tmpdir

( cd $tmpdir && docker build --build-arg version=$version --build-arg app=$app -t $app:$version -t $app:latest . )

#exec docker run --rm -it $app:$version bash
docker run --rm --name $app -t -p 64064:8000 \
    -v $rootProjectDir/events:/srv/events \
    $app:$version
