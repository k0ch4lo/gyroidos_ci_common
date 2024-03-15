def call(Map target = [:]) {
	stepWipeWs(target.workspace)

	//temporary
	sh 'pwd && rm -fr .manifests && git clone https://github.com/gyroidos/gyroidos -b kirkstone .manifests'

	sh label: 'Repo init', script: """
		pwd

		cd ${target.workspace}

		pwd
	
		id

		ls -al /

		ls -al /home

		cd ${target.workspace}/.manifests
		git rev-parse --verify jenkins-ci && git branch -D jenkins-ci
		git checkout -b "jenkins-ci"
		cd ${target.workspace}

		repo init --depth=1 -u ${target.workspace}/.manifests/.git -b "jenkins-ci" -m yocto-${target.gyroid_arch}-${target.gyroid_machine}.xml
	"""

	// TODO test port with actual PRs
	sh label: 'Parse PRs + repo sync', script: '''
		meta_repos="meta-trustx|meta-trustx-intel|meta-trustx-rpi|meta-trustx-nxp"
		cml_repo="cml"
		build_repo="gyroidos_build"
		branch_regex="PR-([0-9]+)"

		echo $branches | tr ',' '\n' | while read -r line; do
			if [[ "$line" =~ ($meta_repos)=$branch_regex ]]; then
				project="${BASH_REMATCH[1]}"
				revision="refs/pull/${BASH_REMATCH[2]}/head"

				echo "\
<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?>\n\
<manifest>\n\
<remove-project name=\\\"$project\\\" />\n\
<project path=\\\"$project\\\" name=\\\"$project\\\" remote=\\\"gyroidos\\\" revision=\\\"$revision\\\" />\n\
</manifest>" >> .repo/local_manifests/$project.xml
			elif [[ "$line" =~ ($cml_repo)=$branch_regex ]]; then
				project="${BASH_REMATCH[1]}"
				revision="refs/pull/${BASH_REMATCH[2]}/head"

				echo "\
<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?>\n\
<manifest>\n\
<remove-project name=\\\"$project\\\" />\n\
<project path=\\\"trustme/cml\\\" name=\\\"$project\\\" remote=\\\"gyroidos\\\" revision=\\\"$revision\\\" />\n\
</manifest>" >> .repo/local_manifests/$project.xml
			elif [[ "$line" =~ ($build_repo)=$branch_regex ]]; then
				project="${BASH_REMATCH[1]}"
				revision="refs/pull/${BASH_REMATCH[2]}/head"

				echo "\
<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?>\n\
<manifest>\n\
<remove-project name=\\\"$project\\\" />\n\
<project path=\\\"trustme/build\\\" name=\\\"$project\\\" remote=\\\"gyroidos\\\" revision=\\\"$revision\\\" />\n\
</manifest>" >> .repo/local_manifests/$project.xml
			else
				echo "Could not parse revision for line $line"
			fi
		done

		repo sync -j8 --current-branch --fail-fast

	'''


	stash includes: "meta-*/**, poky/**, trustme/**", name: "${BUILD_NUMBER}-ws-yocto", useDefaultExcludes: false, allowEmpty: false
	stash includes: ".manifests/**", name: "${BUILD_NUMBER}-manifests", useDefaultExcludes: false, allowEmpty: false
}
