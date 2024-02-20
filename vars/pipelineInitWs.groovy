def call(Map target = [:]) {
	pipeline {
		agent {
            dockerfile {
                dir '.manifests'
                additionalBuildArgs '--build-arg=BUILDUSER=$BUILDUSER'
                args '--entrypoint=\'\' --env BUILDNODE="${env.NODE_NAME}"'
                reuseNode true
            }
        }

		stages {
			stage ("Repo + PR parsing"){
				steps {
					echo "Working on manifest ${target.manifest}"
		
					sh 'pwd'
		
					extws (target.workspace) {
						sh 'echo "Working on workspace at ${target.workspace}'
		
		                sh label: 'Clean workspace and repo init', script: '''
		                    #!/bin/bash
		                    #echo "Running on $(hostname)"
		                    echo "Running on host: ${NODE_NAME}"
		                    rm -fr ${WORKSPACE}/.repo ${WORKSPACE}/*
		
		
		                    env
		
		                    cat .manifests/Dockerfile
		
		                    cd ${WORKSPACE}/.manifests
		                    git rev-parse --verify jenkins-ci && git branch -D jenkins-ci
		                    git checkout -b "jenkins-ci"
		                    cd ${WORKSPACE}
		
		                    repo init --depth=1 -u ${WORKSPACE}/.manifests/.git -b "jenkins-ci" -m yocto-${GYROID_ARCH}-${GYROID_MACHINE}.xml
		
		                    mkdir -p .repo/local_manifests
		
		                    branches="${PR_BRANCHES}"
		
							//TODO add branch parsing
		                    repo sync -j8 --current-branch --fail-fast
						'''
					}
				}
			}
		}
	}
}
