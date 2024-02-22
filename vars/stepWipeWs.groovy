def call(String workspace) {
	echo "Wiping workspace at ${workspace}"

	sh "rm -fr ${workspace}/.repo ${workspace}/meta-* ${workspace}/out-* ${workspace}/trustme/build ${workspace}/poky"

	//TODO remove
	sh "rm -fr trustme"
}
