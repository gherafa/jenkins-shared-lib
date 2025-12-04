pipeline {
    agent {
        docker {
            image 'docker:24-dind'
            args '-v /var/run/docker.sock:/var/run/docker.sock'
        }
    }

    environment {
        REGISTRY       = '__REGISTRY__'
        IMAGE          = '__IMAGE__'
        TAG            = '__TAG__'
    }

    stages {

        stage('Checkout App Code') {
            steps {
                git branch: '__APP_BRANCH__', url: '__APP_REPO_URL__'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "docker build -t ${IMAGE}:${TAG} ."
            }
        }

        stage('Login to GHCR') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: '__GHCR_CREDS__',
                    usernameVariable: 'GH_USER',
                    passwordVariable: 'GH_PAT'
                )]) {
                    sh "echo \$GH_PAT | docker login ghcr.io -u \$GH_USER --password-stdin"
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                sh "docker push ${IMAGE}:${TAG}"
            }
        }

        stage('Checkout Deployment Repo') {
            steps {
                dir('deployments') {
                    git url: '__DEPLOY_REPO__', branch: 'master'
                }
            }
        }

        stage('Generate Deployment YAML') {
            steps {
                script {
                    def deploymentPath = "deployments/__DEPLOY_PATH__/deployment.yaml"
                    def yaml = readFile(deploymentPath)

                    def newYaml = yaml.replaceAll(
                        "(image:\\s*)(.*)",
                        "\$1${IMAGE}:${TAG}"
                    )

                    writeFile(file: "deployment-generated.yaml", text: newYaml)
                }
            }
        }

       stage('Deploy to Kubernetes') {
            steps {
                sh '''
                docker run --rm \
                    -v $PWD:/workspace \
                    -v $HOME/.kube:/root/.kube \
                    lachlanevenson/k8s-kubectl:latest \
                    apply -f /workspace/deployment-generated.yaml
                '''
            }
        }
    }
}
