pipeline {
    agent any

    environment {
        REGISTRY = "ghcr.io/${config.org}"
        IMAGE = "${REGISTRY}/${config.repo}"
        TAG = "${config.tag ?: 'latest'}"
    }

    stages {

        stage('Checkout App Code') {
            steps {
                checkout scm
            }
        }

        stage('Install Dependencies') {
            steps {
                sh "npm install"
            }
        }

        stage('Run Tests') {
            steps {
                sh "npm test"
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
                    credentialsId: config.ghcrCreds,
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
                    git url: config.deployRepo, branch: 'master'
                }
            }
        }

        stage('Generate Deployment YAML') {
            steps {
                script {
                    def path = "deployments/${config.deployPath}/deployment.yaml"
                    def yaml = readFile(path)

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
                sh "kubectl apply -f deployment-generated.yaml"
            }
        }
    }
}
