#!/bin/bash
[ "${TRAVIS_REPO_SLUG}" = "fschopp/java-types" ] \
    && [ "${TRAVIS_BRANCH}" = "master" ] \
    && [ "${TRAVIS_JDK_VERSION}" = "oraclejdk8" ] \
    && [ "${TRAVIS_PULL_REQUEST}" = "false" ]
