---
layout: docs
title: Publishing the documentation
---
# Publishing the documentation

The documentation for Socko is hosted on [GitHub
Pages](http://pages.github.com), with the gh-pages branch of the repo
containing the documentation for multiple versions at once.

This document explains how to publish documentation for the project.

## The publish script

The socko-docs/publish.sh script can be used to publish documentation to the
[project site](http://mashupbots.github.com/socko) for either the current branch or a tag. It works in
the following way:
- Finds the current branch or tag if available
- Checks out the gh-pages branch
- Use git archive to export the socko-docs/docs directory from the identified
  branch or tag to a docs directory namespaced by the branch/tag name


