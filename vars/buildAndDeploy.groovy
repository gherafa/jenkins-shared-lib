def call(Map config = [:]) {

    // --- VALIDATION ---
    if (!config.org)  { error "Missing config.org (GitHub org or username)" }
    if (!config.ghcrCreds) { error "Missing config.ghcrCreds (GHCR credential ID)" }

    def registry = "ghcr.io/${config.org}"
    def branch = config.branch ?: "master"
    def service = config.service

    // Load the declarative pipeline from resources
    def pipelineScript = libraryResource('buildAndDeployScript.groovy')

    def repos = [
        'ai-llm-learn-japanese-service': 'https://github.com/gherafa/ai-llm-learn-japanese-service.git',
        'vue-japanese-speech-recog-app': 'https://github.com/gherafa/vue-japanese-speech-recog-app.git',
        'java-spring-transactions':      'https://github.com/gherafa/java-spring-transactions.git'
    ]

    // Replace placeholders in the pipeline with actual values
    pipelineScript = pipelineScript
        .replace('__REGISTRY__', registry)
        .replace('__GHCR_CREDS__', config.ghcrCreds)
        .replace('__APP_BRANCH__', branch)
        .replace('__IMAGE_TAG__', '${params.IMAGE_TAG}')
        .replace('__SERVICE_NAME__', repos[service])

    // Execute the pipeline
    evaluate(pipelineScript)
}