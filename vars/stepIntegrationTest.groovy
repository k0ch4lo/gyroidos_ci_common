def call(String workspace) {
	echo "Wiping workspace at ${workspace}"
	stepWipeWs(workspace)


	unstash 'ws-yocto'

	script {
		step ([$class: 'CopyArtifact',
			projectName: '/playground/shared_lib_test',
			selector: latestSavedBuild(),
			filter: "out-dev/**/trustmeimage.img.xz",
			flatten: true]);
	}

	//TODO remove
	sh "ls -al ${workspace}"
}
