#!/bin/sh

# get current branch and tag
branch=`git symbolic-ref HEAD 2>/dev/null | cut -d"/" -f 3`;
tag=`git describe --tags 2>/dev/null`;

# if current tag has annotations showing number of commits since tagging, then use current branch
destination="$tag";
if [[ -z $tag ]]; then
  echo "No tags, using branch name as doc destination";
  destination="$branch";
elif [[ "$tag" == *-* ]]; then
  echo "Extra commits since last tag, using branch name as doc destination";
  destination="$branch";
fi
# source and destination will be same, separate variables for clarity
src_tag_or_branch=$destination;

# then git checkout gh-pages and go to top level dir
git checkout gh-pages && cd $(git rev-parse --show-toplevel)

checked_out_branch=`git symbolic-ref HEAD 2>/dev/null | cut -d"/" -f 3`;

if [[ $checked_out_branch -ne "gh-pages" ]]; then
  echo "gh-pages branch not checked out, check that it exists locally";
  exit;
fi

# remove then mkdir for current branch or tag
rm -rf docs/$destination
mkdir -p docs/$destination

# then copy doc files to current branch under a directory for current tag/branch
git archive --format=tar --prefix=docs/$destination/ $src_tag_or_branch:socko-docs/docs | tar -xf -

# then commit new docs with message
git add .
git commit -m "updated docs from $src_tag_or_branch to docs/$destination"

# return to original branch
git checkout $branch

