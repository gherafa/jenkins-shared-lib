pipeline {
    agent {
        docker {
            image 'docker:24-dind'
            args '-v /var/run/docker.sock:/var/run/docker.sock'
        }
    }

    environment {
        REGISTRY = '__REGISTRY__',
        SERVICE_NAME = '__SERVICE_NAME__',
        IMAGE_TAG = '__IMAGE_TAG__',
    }

    stages {
         stage('Detect Language') {
            steps {
                script {
                    if (fileExists("package.json")) {
                        env.SERVICE_TYPE = "node"
                    } else if (fileExists("requirements.txt") || fileExists("pyproject.toml")) {
                        env.SERVICE_TYPE = "python"
                    } else {
                        env.SERVICE_TYPE = "other"
                    }

                    echo "Detected service type: ${env.SERVICE_TYPE}"
                }
            }
        }

        stage('Checkout App Code') {
            steps {
                git branch: '__APP_BRANCH__', url: '__SERVICE_NAME__'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh """
                docker build -t ${REGISTRY}/${SERVICE_NAME}:${IMAGE_TAG} .
                """
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
                    sh """
                    docker push ${REGISTRY}/${SERVICE_NAME}:${IMAGE_TAG}
                    """
            }
        }
    }
}
