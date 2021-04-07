    import hudson.model.*
    import hudson.plugins.git.Branch
    import hudson.plugins.git.Revision
    import hudson.plugins.git.util.BuildData
    import java.util.regex.Pattern
    /**
     * Wrapper for checking if loaded jenkins shared libs are pointing to a git branch or tag
     *
     * @return Boolean
     */
    Boolean isLockedSharedLibraryRevision() {
        List<Action> actions = $build().getActions(BuildData.class)

        return checkSharedLibraryBranches(actions)
    }

    /**
     * Check if shared libraries are locked to specific git tag (commit hash)
     * Return True if running on a particular revision (Git Tag)
     * Return False if running on HEAD of a branch (develop by default)
     *
     * Assumption is that Git Tag follows format vx.x.x (e.g. v1.0.22)
     *
     * @param actions (List of jenkins actions thatmatch BuildData.class)
     * @return Boolean
     */
    Boolean checkSharedLibraryBranches(List<Action> actions) {
        Boolean isLockedSharedLibraryRevision = false
        Boolean jenkinsSharedFound = false
        if (actions == null || actions.size() == 0) {
            throw new IllegalArgumentException("Build actions must be provided")
        }
        // Check each BuildData Action returned for one containing the jenkins-shared revisions
        actions.each { action ->
            HashSet remoteURLs = action.getRemoteUrls()
            remoteURLs.each { url ->
                if ( url.contains('jenkins-shared-library') ) {
                    jenkinsSharedFound = true
                    println "INFO: Jenkins-shared find"
                    Pattern versionRegex = ~/^\d+\.\d+\.\d+$/
                    /**
                     * When jenkins-shared is found evaluate revision branch/tag name.
                     * getLastBuiltRevision() returns the current executions build. This was functionally tested.
                     * If a newer build runs and completes before the current job, the value is not changed.
                     * i.e. Build 303 starts and is in progress, build 304 starts and finishes.
                     * Build 303 calls getLastBuiltRevision() which returns job 303 (not 304)
                     */
                    Revision revision = action.getLastBuiltRevision()
                    String sha = revision.getSha1String()
                    println "sha is ${sha}"
                    /**
                     * This is always a collection of 1, even when multiple tags exist against the same sha1 in git
                     * It is always the tag/branch your looking at and doesn't report any extras...
                     * Despite this we loop to be safe
                     */
                    Collection<Branch> branches = revision.getBranches()
                    branches.each { branch ->
                        String name = branch.getName()
                        println "branch name is ${name}"
                        if (name ==~ /\d+\.\d+\.\d+/) {
                            println "INFO: Jenkins-shared locked to version ${name}"
                            isLockedSharedLibraryRevision = true
                        }
                    }
                }
            }
        }

        if (!jenkinsSharedFound) {
                throw new IllegalArgumentException("None of the related build actions have a remoteURL pointing to Jenkins Shared, aborting")
        }

        println "INFO: isLockedSharedLibraryRevision == ${isLockedSharedLibraryRevision}"
        return isLockedSharedLibraryRevision
    }
