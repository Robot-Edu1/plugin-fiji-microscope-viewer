# SPDX-License-Identifier: MIT
PYTHON ?= python3
IJ_JAR ?=
TAG ?= v$(shell cat VERSION)
export IJ_JAR
.PHONY: all test release
all:
	$(PYTHON) tools/build.py
test: all
	$(PYTHON) tools/test.py
release:
	$(PYTHON) tools/release.py --tag "$(TAG)"
