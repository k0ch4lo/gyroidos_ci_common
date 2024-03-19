def call(Map target) {
	//sh label: 'Clean up workspace', script: """
	//	find "${target.workspace}" -mindepth 1 -exec rm -fr {} \\;
	//"""

	echo "Running on host: ${NODE_NAME}"

	// workaround for missing ability to retrieve build number from CopyArtifacts plugin as suggested in JENKINS-34620
	writeFile file: "${target.workspace}/.build_no", text: "${BUILD_NUMBER}"


	stepWipeWs(target.workspace)

	unstash "${BUILD_NUMBER}-ws-yocto"
	unstash "${BUILD_NUMBER}-manifests"

	sh label: 'Perform Yocto build', script: """
		export LC_ALL=en_US.UTF-8
		export LANG=en_US.UTF-8
		export LANGUAGE=en_US.UTF-8

		if [ "dev" = ${target.buildtype} ];then
			echo "Preparing Yocto workdir for development build"
			SANITIZERS=y
		elif [ "production" = "${target.buildtype}" ];then
			echo "Preparing Yocto workdir for production build"
			DEVELOPMENT_BUILD=n
		elif [ "ccmode" = "${target.buildtype}" ];then
			echo "Preparing Yocto workdir for CC Mode build"
			DEVELOPMENT_BUILD=n
			ENABLE_SCHSM="1"
			CC_MODE=y
		elif [ "schsm" = "${target.buildtype}" ];then
			echo "Preparing Yocto workdir for dev mode build with schsm support"
			SANITIZERS=y
			ENABLE_SCHSM="1"
		else
			echo "Error, unkown ${target.buildtype}, exiting..."
			exit 1
		fi

		if [ -d out-${target.buildtype}/conf ]; then
			rm -r out-${target.buildtype}/conf
		fi

		. trustme/build/yocto/init_ws_ids.sh out-${target.buildtype} ${target.gyroid_arch} ${target.gyroid_machine}

		cd ${target.workspace}/out-${target.buildtype}

		echo "INHERIT += \\\"own-mirrors\\\"" >> conf/local.conf
		echo "SOURCE_MIRROR_URL = \\\"file:///source_mirror\\\"" >> conf/local.conf
		echo "BB_GENERATE_MIRROR_TARBALLS = \\\"1\\\"" >> conf/local.conf

		echo "SSTATE_MIRRORS =+ \\\"file://.* file:///sstate_mirror/${target.buildtype}/PATH\\\"" >> conf/local.conf

		echo "BB_SIGNATURE_HANDLER = \\\"OEBasicHash\\\"" >> conf/local.conf
		echo "BB_HASHSERVE = \\\"\\\"" >> conf/local.conf

		cat conf/local.conf


		echo 'TRUSTME_DATAPART_EXTRA_SPACE="3000"' >> conf/local.conf

		bitbake trustx-cml-initramfs multiconfig:container:trustx-core
		bitbake trustx-cml

		if [ "y" = "${target.build_installer}" ];then
			 bitbake multiconfig:installer:trustx-installer
		fi
	"""

	sh label: 'Compress trustmeimage.img', script: "xz -T 0 -f out-${target.buildtype}/tmp/deploy/images/*/trustme_image/trustmeimage.img --keep"

	//TODO move scrips/ci to shared lib
	stash includes: "out-${target.buildtype}/test_certificates/**, meta-trustx/**, trustme/build/**, meta-trustx/scripts/ci/**", excludes: "**/oe-logs/**, **/oe-workdir/**", name: "${BUILD_NUMBER}-buildout-${target.buildtype}"	

	script {
		if (target.build_installer && "y" == target.build_installer) {
			sh label: 'Compress trustmeinstaller.img', script: "xz -T 0 -f out-${target.buildtype}/tmp/deploy/images/**/trustme_image/trustmeinstaller.img --keep"
		}
	}

	archiveArtifacts artifacts: "out-${target.buildtype}/tmp/deploy/images/**/trustme_image/trustmeimage.img.xz, \
				       out-${target.buildtype}/tmp/deploy/images/**/trustme_image/trustmeinstaller.img.xz, \
				       out-${target.buildtype}/test_certificates/**, \
				       out-${target.buildtype}/tmp/deploy/images/**/ssh-keys/**, \
				       out-${target.buildtype}/tmp/deploy/images/**/cml_updates/kernel-**.tar, .buildNumber" , fingerprint: true

		script {
			if (target.sync_mirrors && "y" == target.sync_mirrors) {
				lock ('sync-mirror') {
					script {
						catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
							sh label: 'Syncing mirrors', script: """
								if [ -d "/source_mirror" ];then
									rsync -r --ignore-existing --no-devices --no-specials --no-links  out-${target.buildtype}/downloads/ /source_mirror
								else
									exit 1
								fi

								if [ -d "/sstate_mirror" ];then
									rsync -r --no-devices --no-specials --no-links out-${target.buildtype}/sstate-cache/ /sstate_mirror/${target.buildtype}
								else
									echo "Skipping sstate_mirror sync, /sstate_mirror is missing"
									exit 1
								fi

								exit 0
							"""
						}
					}
				}
			} else {
				echo "Skipping sstate cache sync"
			}
		}
}
