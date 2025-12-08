pipeline {
    agent {
        docker {
            image 'docker:24-dind'
            args '-v /var/run/docker.sock:/var/run/docker.sock'
        }
    }

    environment {
        REGISTRY       = '__REGISTRY__'
    }

    parameters {
        choice(name: 'SERVICE_NAME', choices: ['ai-llm-learn-japanese-service', 'vue-japanese-speech-recog-app', 'java-spring-transactions'], description: 'Select service')
        string(name: 'BRANCH', defaultValue: 'main', description: 'Branch to build')
        string(name: 'IMAGE_TAG', defaultValue: 'latest', description: 'Docker image tag')
    }

    stages {
        stage('Select Repo') {
            steps {
                script {
                    def repos = [
                        'ai-llm-learn-japanese-service': 'https://github.com/gherafa/ai-llm-learn-japanese-service.git',
                        'vue-japanese-speech-recog-app': 'https://github.com/gherafa/vue-japanese-speech-recog-app.git',
                        'java-spring-transactions': 'https://github.com/gherafa/java-spring-transactions.git',
                    ]

                    env.SELECTED_REPO = repos[params.SERVICE_NAME]
                    echo "Selected repo: ${env.SELECTED_REPO}"
                }
            }
        }

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
                git branch: '__APP_BRANCH__', url: env.SELECTED_REPO
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
            sh """
            docker push ${REGISTRY}/${SERVICE_NAME}:${IMAGE_TAG}
            """
        }
    }
}
