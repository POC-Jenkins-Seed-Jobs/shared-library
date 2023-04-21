def installNodeJs(){
    sh "sudo apt install nodejs -y"
    // sh "node_version=$(node -v)"
    // echo "Success installed Node.js ${node_version}"
}
def installNpm(){
    sh "sudo apt install npm -y"
}
def gitCheckout(url){
    checkout scmGit(branches: [[name: 'main']], extensions: [], userRemoteConfigs: [[url: "${url}"]])
    echo "Code Cloned Successfully !"
}

def nodeJsBuild(){
    sh "npm install"
    echo "Build Successfully !"
}

def nodeJsTest(){
    sh "npm run test"
    echo "Test Successfully !"
}

def TriggerAnotherJob(AppName)
{
    sh '''
    build job :"${AppName}"
    '''
}
    
def dockerBuildAndPush(dockerRegistry,credentialsId,imageName){
    withDockerRegistry(credentialsId: "${credentialsId}", url: "${dockerRegistry}") {
        sh '''
            epochTime=$(date +%s)
            docker build -t '''+"${imageName}"+''':${epochTime} .
            docker push '''+"${imageName}"+''':${epochTime}
            docker image tag '''+"${imageName}"+''':${epochTime} '''+"${imageName}"+''':latest
            docker push '''+"${imageName}"+''':latest
            
        '''
        // sh "docker build -t ${imageName}:${BUILD_NUMBER} ."
        // sh "docker push ${imageName}:${BUILD_NUMBER}"
    }
}

def testDSL(name){
    jobDsl scriptText: '''
        pipelineJob('''+"\"${name}\""+''') {
            parameters {
                stringParam(\'repoURL\',\'\',\'Repository URL of the Project\' )
                stringParam(\'dockerRegistry\',\'\', \'Docker Registry Login URL\' )
                stringParam(\'credentialsId\',\'\',\'Credentials ID of Docker Registry Saved in Jenkins\' )
                stringParam(\'imageName\',\'\',\'Provide Full Name of the Image to be Build\' )
                
            }
            definition{
                cps{
                    script(\'\'\'
                        @Library(\'shared-library-nilanjan\') _
                        pipeline{
                            agent{
                                label "ec2-slave"
                            }
                            stages{
                                stage("Get Sources"){
                                    steps{
                                        script{
                                            build.gitCheckout("${repoURL}")
                                        }
                                    }
                                }
                                stage("Install Node.Js"){
                                    steps{
                                        script{
                                            build.installNodeJs()
                                        }
                                    }
                                }
                                stage("Install NPM"){
                                    steps{
                                        script{
                                            build.installNpm()
                                        }
                                    }
                                }
                                stage("Build"){
                                    steps{
                                        script{
                                            build.nodeJsBuild()
                                        }
                                    }
                                }
                                stage("Test"){
                                    steps{
                                        script{
                                            build.nodeJsTest()
                                        }
                                    }
                                }
                                stage("Docker Build & Push"){
                                    steps{
                                        script{
                                            build.dockerBuildAndPush("${dockerRegistry}","${credentialsId}","${imageName}")
                                        }
                                    }
                                }
                                
                            }
                            
                        }
                    \'\'\'.stripIndent())
                    sandbox()
                }
            }
        }'''.stripIndent()
}

def testDSL2(jobName, appName, imageName){
    jobDsl scriptText: '''
        pipelineJob('''+"\"${jobName}\""+''') {
            definition{
                cps{
                    script(\'\'\'
                        @Library(\'shared-library-nilanjan\') _
                        pipeline{
                            agent{
                                label "ec2-slave"
                            }
                            stages{
                                stage("Get Sources"){
                                    steps{
                                        script{
                                            build.gitCheckout("https://github.com/Team-Denver/POC.Service.'''+"${appName}"+'''.git")
                                        }
                                    }
                                }
                                
                                stage("Build"){
                                    steps{
                                        script{
                                            build.nodeJsBuild()
                                        }
                                    }
                                }
                                stage("Test"){
                                    steps{
                                        script{
                                            build.nodeJsTest()
                                        }
                                    }
                                }
                                stage("Docker Build & Push"){
                                    steps{
                                        script{
                                            build.dockerBuildAndPush("https://index.docker.io/v1/","nilanjan-docker",'''+"\"${imageName}\""+''')
                                        }
                                    }
                                }
                                
                            }
                            
                        }
                    \'\'\'.stripIndent())
                    sandbox()
                }
            }
        }'''.stripIndent()
}
