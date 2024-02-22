def call(String workspace) {
	script {
		sh 'echo "Performing unit tests"'
	}

	sh label: 'Clean CML Repo', script: "git -C ${workspace}/trustme/cml clean -fx"

	sh label: 'Perform unit tests', script: "${workspace}/meta-trustx/scripts/ci/unit-testing.sh"
}
