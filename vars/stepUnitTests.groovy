def call(String sourcedir) {
	// This step executes the unit tests in libcommon
	// However, it can be extended and used for any pre-build-time task (fuzzing, other tests, etc).
	
	script {
		sh 'echo "Performing unit tests"'
	}

	sh label: 'Clean CML Repo', script: "git -C ${sourcedir} clean -fx"

	testscript = libraryResource('unit-testing.sh')	
	writeFile file: "${WORKSPACE}/unit-testing.sh", text: "${testscript}"

	sh label: 'Perform unit tests', script: "bash ${WORKSPACE}/unit-testing.sh ${sourcedir}"
}
