package org.snaatak

class GoUnitTesting implements Serializable {
    def steps

    GoUnitTesting(steps) {
        this.steps = steps
    }

    def checkoutCode(String branch = 'main') {
        def repoUrl = steps.env.REPO_URL
        def credentialsId = steps.env.CREDENTIAL_ID

        steps.stage('Checkout') {
            steps.echo "Checking out branch '${branch}' from '${repoUrl}'"
            steps.checkout([$class: 'GitSCM',
                branches: [[name: "*/${branch}"]],
                userRemoteConfigs: [[
                    url: repoUrl,
                    credentialsId: credentialsId
                ]]
            ])
        }
    }

    def installGoJunitReport() {
        steps.stage('Install go-junit-report') {
            steps.sh 'go install github.com/jstemmer/go-junit-report@latest'
        }
    }

    def runTestsAndGenerateReports() {
        steps.stage('Run Tests and Generate Reports') {
            steps.sh '''
                export PATH=$HOME/go/bin:$PATH
                mkdir -p test-reports coverage-reports
                go test -v -coverprofile=coverage-reports/coverage.out ./... | go-junit-report > test-reports/report.xml
                go tool cover -html=coverage-reports/coverage.out -o coverage-reports/coverage.html
            '''
        }
    }

    def publishJUnitReport() {
        steps.stage('Publish JUnit Report') {
            steps.junit 'test-reports/report.xml'
        }
    }

    def publishCoverageReport() {
        steps.stage('Publish Report') {
            steps.publishHTML(target: [
                reportDir: 'coverage-reports',
                reportFiles: 'coverage.html',
                reportName: 'Go Report',
                keepAll: true
            ])
        }
    }

    def archiveAndCleanup() {
        steps.stage('Cleanup') {
            steps.echo "Cleaning up workspace"
        }
    }
}
