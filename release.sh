#!/bin/bash
gh workflow run release.yml --ref master
echo https://github.com/illepidus/diasync/actions/workflows/release.yml
