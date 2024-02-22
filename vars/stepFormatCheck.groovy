def call(String workspace) {
	sh 'echo "Checking formatting"'

	sh label: 'Clean CML Repo', script: "git -C ${workspace}/trustme/cml clean -fx"
	
	sh label: 'Check code formatting', script: "${workspace}/meta-trustx/scripts/ci/check-if-code-is-formatted.sh"
}
