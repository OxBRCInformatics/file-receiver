# File Receiver

[![Build Status](https://travis-ci.org/OxBRCInformatics/file-receiver.svg)](https://travis-ci.org/OxBRCInformatics/file-receiver)
[![Master](http://img.shields.io/badge/master-1.0-green.svg)](https://github.com/OxBRCInformatics/file-receiver/tree/master)
[![License](http://img.shields.io/badge/license-Apache_V2.0_License-lightgrey.svg)](https://github.com/OxBRCInformatics/file-receiver/blob/develop/LICENSE)

## Release

To release, use gradle to build and deploy, if you have gradle installed then use as normal if not then use `./gradlew`

1. Firstly use `git flow` to create a new `release` with the next version number.
1. Change the version in the relevant `gradle.properties` file.
1. Commit the update
1. Finish the `git flow` release and tag with the version number
1. git checkout the master branch
1. `gradle test` run a final test
1. `gradle bintrayUpload --info` build and deploy to Bintray

You will need to set system environment variables

* `BINTRAY_USER` - your Bintray username
* `BINTRAY_KEY` - your Bintray API key

Assuming `test` completes then run the `bintrayUpload` command to build the `jar`s and `tar` files and then upload them
to Bintray.