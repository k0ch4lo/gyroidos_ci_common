def call(Map target) {
	echo "Running on host: ${NODE_NAME}"

	stepWipeWs(target.workspace)

	script {
		if ((! target.containsKey("workspace")) || (! target.containsKey("buildtype")) || (! target.containsKey("schsm_serial")) || (! target.containsKey("schsm_pin"))) {
			error("Missing keys in map 'target'")
		}

		step ([$class: 'CopyArtifact',
			projectName: '/playground/shared_lib_test',
			selector: target.buildSelector,
			filter: "out-${target.buildtype}/**/trustmeimage.img.xz, out-${target.buildtype}/test_certificates/**",
			flatten: true]);
//			selector: latestSavedBuild(),


		unstash "${artifact_build_no}-buildout-${target.buildtype}"

		echo "Using stash of build number determined by selector: ${artifact_build_no}"

		def artifact_build_no = readFile file: "${target.workspace}/.build_no"

		sh label: "Extract image", script: 'unxz -T0 trustmeimage.img.xz'

		if (target.schsm_serial) {
			schsm_opts="--enable-schsm ${target.schsm_serial} ${target.schsm_pin}"
			test_mode="dev"
		} else {
			schsm_opts=""
			test_mode="${target.buildtype}"
		}
		
		sh label: "Perform integration test", script: """
			${target.workspace}/meta-trustx/scripts/ci/VM-container-tests.sh --mode "${test_mode}" --dir "${target.workspace}" --image trustmeimage.img --pki "${target.workspace}/test_certificates" --name "testvm" --ssh 2222 --kill --vnc 1 --log-dir "${target.workspace}/out-"${target.buildtype}"/cml_logs" ${schsm_opts}
		"""
	}

	echo "Archiving CML logs"
	archiveArtifacts artifacts: 'out-**/cml_logs/**, cml_logs/**', fingerprint: true, allowEmptyArchive: true
}
