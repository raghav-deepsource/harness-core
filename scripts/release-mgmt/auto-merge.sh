#!/bin/bash
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

#Merging Strategy: Fast-Forward Rebasing

function print_err(){
    local_cmd_status=$1
    local_cmd_name=$2
    if [ "$local_cmd_status" != 0 ]; then
        echo "ERROR: Line $LINENO: $2 Command Failed... Exiting..."
        exit 1
    fi
}

GIT_COMMAND="git log --graph --abbrev-commit --date=relative"

if [ -z "${BOT_USER}" ] && [ -z "${BOT_PWD}" ] && [ -z "${BOT_EMAIL}" ]; then
    echo "ERROR: Line: $LINENO: BOT Parameters are missing. Exiting..."
    exit 1
fi

echo "STEP 1: Setting up remote."
git remote set-url origin https://${BOT_USER}:${BOT_PWD}@github.com/harness/harness-core.git; git remote -v && git branch

echo "STEP 2: Setting Git Username and Password"
git config --global user.name "${BOT_USER}"
git config --global user.email "${BOT_EMAIL}"

git fetch --unshallow

#BT-988: Merge Pre-QA validated changes to merge to master
if [ -z "${MANUAL_TRIGGER}" ] ; then
  pre_qa_content=$(wget https://stress.harness.io/api/version -q -O -)
  gitCommit=$(echo "$pre_qa_content" | jq -r '.resource.versionInfo.gitCommit')
  git branch temp_branch $gitCommit
else
  git checkout -b temp_branch develop
fi

echo "STEP 3: Checking out Master to local repo."
git checkout master && git branch

echo "STEP 4: Checking if Master branch is ahead of temp branch"
MASTER_TO_TEMP_BRANCH=$($GIT_COMMAND temp_branch..master)
echo "INFO: Diff: $MASTER_TO_TEMP_BRANCH"
if [ ! -z "$MASTER_TO_TEMP_BRANCH" ]; then
    echo "ERROR: Line $LINENO: Master branch is ahead of temp. Exiting..."
    exit 1
fi

echo "STEP 4: Rebasing temp_branch branch to Master branch"
git rebase temp_branch #If we fire command in STEP 3
print_err "$?" "Rebasing"

# git rebase develop master #Does git checkout master and git rebase develop
# print_err "$?" "Rebasing"

#NOTE: Both commands in STEP 5 should give empty results to make sure that both have same commits.
echo "STEP 5: Matching the commits in master and develop"
MASTER_TO_TEMP=$($GIT_COMMAND temp_branch..master)
TEMP_TO_MASTER=$($GIT_COMMAND master..temp_branch)

if [ ! -z "$MASTER_TO_TEMP" ] && [ ! -z "$TEMP_TO_MASTER" ]; then
    echo "ERROR: Line $LINENO: Master and Develop branches are not identical after rebasing. Exiting..."
    exit 1
fi

echo "STEP 6: Pushing to target branch: master"
if [ -z "$MASTER_TO_TEMP" ] && [ -z "$TEMP_TO_MASTER" ]; then
    git push origin master
    print_err "$?" "Push to Master"
fi
