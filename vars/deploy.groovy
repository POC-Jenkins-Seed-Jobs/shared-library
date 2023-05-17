def deleteDeployment(){
    sh "kubectl delete deployment deployment"
}
def applyk8sManifest(){
    sh "kubectl apply -f deployment.yml"
}
def deployToKubernetes(name, appName){
    jobDsl scriptText: '''
        pipelineJob('''+"\"${name}\""+''') {
            definition{
                cps{
                    script(\'\'\'
                        @Library(\'shared-library-nilanjan\') _
                        pipeline{
                            agent{
                                label "ec2-slave"
                            }
                            stages{
                                stage("Delete old deployment"){     
                             
                                    steps{
                                        script{
                                            deploy.deleteDeployment()
                                        }
                                    }
                                }
                                stage("Deploy to kubernetes"){

                                   
                                    steps{
                                        script{
                                            deploy.applyk8sManifest()
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



// Some Test Functions 

def updatek8Cluster() {
   withCredentials([aws(accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: 'nilanjan-aws', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    sh '''
                        aws eks update-kubeconfig --region us-east-1 --name poc-adot-with-eks
                    '''
   }
} 
def destroyk8(appname)
    {
        withCredentials([aws(accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: 'nilanjan-aws', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                sh '''kubectl delete deployment '''+"${appname}"+'''
                   '''
                
        }       
    }
    
    def applykubectl()
        {
             withCredentials([aws(accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: 'nilanjan-aws', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                sh'''
                kubectl apply -f Deployment.yml
                '''
             }
        } 
def testDeploy(jobName, appName){
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
                                
                                 stage("Delete deployment"){
                                    steps{
                                        script{
                                            deploy.destroyk8("'''+"${appName}"+'''")
                                        }
                                    }
                                 

                                }
                                stage("Apply deployments"){
                                    steps{
                                         script{
                                            deploy.applykubectl()
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
