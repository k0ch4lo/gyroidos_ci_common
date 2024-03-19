def call(String path) {
	dockerfile = libraryResource('Dockerfile')
	writeFile file: "${path}/Dockerfile", text: "${dockerfile}"	
}
