# Jenkins Shared Library

This is a shared Jenkins library that contains reusable pipeline steps
for building Vue applications.

## Usage

Add this library in Jenkins:

1. Go to **Manage Jenkins → Configure System**
2. Scroll to **Global Pipeline Libraries**
3. Add:
   - Name: `jenkins-shared-library`
   - Default version: `main`
   - GitHub URL: `https://github.com/gherafa/jenkins-shared-library`

## Example Jenkinsfile

```groovy
@Library('jenkins-shared-library') _

vueBuild(
    nodeVersion: '18',
    buildScript: 'build'
)
