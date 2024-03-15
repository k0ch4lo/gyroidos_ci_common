def call(Map target) {
	echo "Wiping target.workspace at ${target.workspace}"
	stepWipeWs(target.workspace)


	//unstash 'ws-yocto'

	script {
		if ((! target.containsKey("workspace")) || (! target.containsKey("buildtype")) || (! target.containsKey("schsm_serial")) || (! target.containsKey("schsm_pin"))) {
			error("Missing keys in map 'target'")
		}

		step ([$class: 'CopyArtifact',
			projectName: '/playground/shared_lib_test',
			selector: latestSavedBuild(),
			filter: "out-dev/**/trustmeimage.img.xz",
			flatten: true]);
	}

	sh "ls -al ${target.workspace}"


	//unstash "${BUILD_NUMBER}-buildout-${target.buildtype}"

	sh label: "Perform integration test", script: """
		unxz -T0 trustmeimage.img.xz

		ls -al /yocto_mirror

		tar -xf /yocto_mirror/teststash.tar

		#TODO  BUILDNODE => NODE_NAME
		echo "Running on host: ${env.BUILDNODE}"
		echo "$PATH"

		if ! [ -z "${target.schsm_serial}" ];then
			SCHSM_OPTS="--enable-schsm ${target.schsm_serial} ${target.schsm_pin}"
		else 
			SCHSM_OPTS=""
		fi
		
		${target.workspace}/meta-trustx/scripts/ci/VM-container-tests.sh --mode "${target.buildtype}" --skip-rootca --dir "${target.workspace}" --image trustmeimage.img --pki "${target.workspace}/out-"${target.buildtype}"/test_certificates" --name "testvm" --ssh 2222 --kill --vnc 1 --log-dir "${target.workspace}/out-"${target.buildtype}"/cml_logs" \${SCHSM_OPTS}
		
	"""
}
