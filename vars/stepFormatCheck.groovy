def call(String sourcedir) {
	echo "Checking formatting for sources at ${sourcedir}, WORKSPACE: ${WORKSPACE}"
	sh label: 'Clean CML Repo', script: "git -C ${sourcedir} clean -fx"

	checkscript = libraryResource('check-if-code-is-formatted.sh')	
	writeFile file: "${WORKSPACE}/check-if-code-is-formatted.sh", text: "${checkscript}"	

	sh label: 'Check code formatting', script: "bash ${WORKSPACE}/check-if-code-is-formatted.sh ${sourcedir}"
}
