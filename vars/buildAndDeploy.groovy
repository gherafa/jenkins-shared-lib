def call(Map config = [:]) {
    // load pipeline from resources folder
    def pipelineScript = libraryResource('buildAndDeployPipeline.groovy')

    // evaluate it as a pipeline, injecting your config into it
    evaluate(pipelineScript)
}
