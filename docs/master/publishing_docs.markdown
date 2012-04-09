---
layout: docs
title: The Socko project site and documentation
---
# The Socko project site

The Socko project is at [http://sockoweb.org](http://sockoweb.org). This site
includes the [documentation for multiple versions of the
software](http://sockoweb.org/docs.html). This page explains where to edit
documentation, how to synchronise that documentation to the project site
branch of the repo, and how to publish the project site.

## Editing the project documentation

Each instance of documentation is generated from the markdown files in the socko-docs/docs
directory. Aside from being markdown files, these files also include
information to drive the Jekyll generation of the project site. This mostly
means that they contain [YAML front
matter](https://github.com/mojombo/jekyll/wiki/YAML-Front-Matter).

## The synchronise script

The socko-docs/synch_docs.sh script is used to synchronise documents to the
[project site](http://mashupbots.github.com/socko) stored in the gh-pages
branch, from either the current branch or a tag. It works in the following
way:

- Finds the current branch or tag if available
- Checks out the gh-pages branch
- Use git archive to export the socko-docs/docs directory from the identified
  branch or tag to a docs directory namespaced by the branch/tag name
- Commits the changes to the gh-pages branch

# Publishing the updated project site

To publish the project site, push the gh-pages branch to GitHub. Socko uses a
custom domain name for the project site, so the updated site will be available
at [http://sockoweb.org](http://sockoweb.org).


