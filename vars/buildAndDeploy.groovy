def call(Map cfg = [:]) {

    // Validate required fields
    if (!cfg.service)     { error "Missing cfg.service" }
    if (!cfg.branch)      { error "Missing cfg.branch" }
    if (!cfg.tag)         { error "Missing cfg.tag" }
    if (!cfg.org)         { error "Missing cfg.org (GitHub org)" }
    if (!cfg.ghcrCreds)   { error "Missing cfg.ghcrCreds (Credential ID)" }

    def repos = [
        'ai-llm-learn-japanese-service': 'https://github.com/gherafa/ai-llm-learn-japanese-service.git',
        'vue-japanese-speech-recog-app': 'https://github.com/gherafa/vue-japanese-speech-recog-app.git',
        'java-spring-transactions':      'https://github.com/gherafa/java-spring-transactions.git'
    ]

    if (!repos[cfg.service]) {
        error "Unknown service name: ${cfg.service}"
    }

    def repoUrl = repos[cfg.service]
    def imageName = "ghcr.io/${cfg.org}/${cfg.service}:${cfg.tag}"


    // ============= STAGE 1: CHECKOUT ==================
    stage("Checkout ${cfg.service}") {
        git branch: cfg.branch, url: repoUrl
    }


    // ============= STAGE 2: DETECT LANGUAGE ==================
    stage('Detect Project Type') {
        script {
            if (fileExists("package.json")) {
                env.SERVICE_TYPE = "node"
            } else if (fileExists("requirements.txt") || fileExists("pyproject.toml")) {
                env.SERVICE_TYPE = "python"
            } else {
                env.SERVICE_TYPE = "other"
            }

            echo "Detected type: ${env.SERVICE_TYPE}"
        }
    }


    // ============= STAGE 3: BUILD IMAGE ==================
    stage("Build Docker Image") {
        sh """
        docker build -t ${imageName} .
        """
    }


    // ============= STAGE 4: LOGIN ==================
    stage("Login to GHCR") {
        withCredentials([usernamePassword(
            credentialsId: cfg.ghcrCreds,
            usernameVariable: 'GH_USER',
            passwordVariable: 'GH_PAT'
        )]) {
            sh """
            echo \$GH_PAT | docker login ghcr.io -u \$GH_USER --password-stdin
            """
        }
    }


    // ============= STAGE 5: PUSH IMAGE ==================
    stage("Push Image") {
        sh """
        docker push ${imageName}
        """
        echo "Pushed image: ${imageName}"
    }
}
