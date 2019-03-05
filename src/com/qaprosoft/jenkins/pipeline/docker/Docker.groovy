package com.qaprosoft.jenkins.pipeline.docker

class Docker {

    def buildDocker(version){
        context.stage('Docker Build') {
            dockerImage = context.docker.build(registry + ":${version}", "--build-arg version=${version} .")
        }
    }
}

//    stage('Deploy Image') {
//        steps{
//            script {
//                docker.withRegistry('', registryCredentials) {
//                    dockerImage.push()
//                }
//            }
//        }
//    }
//}