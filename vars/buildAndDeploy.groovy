def call(Map config = [:]) {
  // --- VALIDATION ---
  if (!config.org)  { error "Missing config.org (GitHub org or username)" }
  if (!config.repo) { error "Missing config.repo (app repo name)" }
  if (!config.ghcrCreds) { error "Missing config.ghcrCreds (GHCR credential ID)" }
  if (!config.deployRepo) { error "Missing config.deployRepo (deployment repo URL)" }
  if (!config.deployPath) { error "Missing config.deployPath (path to deployment.yaml)" }

  // Precompute values to avoid environment block errors
  def registry = "ghcr.io/${config.org}"
  def image = "${registry}/${config.repo}"
  def tag = config.tag ?: "latest"

  // Load the pipeline from resources
  def pipelineScript = libraryResource('buildAndDeployScript.groovy')

  // Replace placeholders in the pipeline with actual values
  pipelineScript = pipelineScript
      .replace('__REGISTRY__', registry)
      .replace('__IMAGE__', image)
      .replace('__TAG__', tag)
      .replace('__GHCR_CREDS__', config.ghcrCreds)
      .replace('__DEPLOY_REPO__', config.deployRepo)
      .replace('__DEPLOY_PATH__', config.deployPath)

  // Execute the pipeline
  evaluate(pipelineScript)
}
