def call(Map config = [:]) {

    // --- VALIDATION ---
    if (!config.org)  { error "Missing config.org (GitHub org or username)" }
    if (!config.ghcrCreds) { error "Missing config.ghcrCreds (GHCR credential ID)" }

    def registry = "ghcr.io/${config.org}"
    def branch = config.branch ?: "master"

    // Load the declarative pipeline from resources
    def pipelineScript = libraryResource('buildAndDeployScript.groovy')

    // Replace placeholders in the pipeline with actual values
    pipelineScript = pipelineScript
        .replace('__REGISTRY__', registry)
        .replace('__GHCR_CREDS__', config.ghcrCreds)
        .replace('__APP_BRANCH__', branch)

    // Execute the pipeline
    evaluate(pipelineScript)
}
