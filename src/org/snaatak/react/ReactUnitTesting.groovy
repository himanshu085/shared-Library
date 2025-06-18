package org.snaatak.react

class ReactUnitTesting implements Serializable {
    def steps

    ReactUnitTesting(steps) {
        this.steps = steps
    }

    def checkoutCode(String branch = 'main', String repoUrl = '', String credentialsId = '') {
        steps.stage('Checkout') {
            steps.echo "Checking out branch '${branch}' from '${repoUrl}'"
            steps.checkout([
                $class: 'GitSCM',
                branches: [[name: "*/${branch}"]],
                userRemoteConfigs: [[url: repoUrl, credentialsId: credentialsId]]
            ])
        }
    }

    def installDependencies() {
        steps.stage('Install Dependencies') {
            steps.dir('frontend') {
                steps.sh '''
                    npm install
                    npm install --save-dev @testing-library/react@12.1.5 @testing-library/jest-dom@5.16.5
                '''
            }
        }
    }

    def runUnitTests() {
        steps.stage('Run Unit Tests') {
            steps.dir('frontend') {
                steps.sh 'npm test -- --watchAll=false > test.log 2>&1 || true'
            }
        }
    }

    def generateCoverage() {
        steps.stage('Generate Report') {
            steps.dir('frontend') {
                steps.sh 'npm test -- --coverage --coverageDirectory=coverage --watchAll=false || true'
            }
        }
    }

    def publishCoverage() {
        steps.stage('Publish Report') {
            steps.publishHTML(target: [
                reportDir: 'coverage/lcov-report',
                reportFiles: 'index.html',
                reportName: 'React Test Report'
            ])
        }
    }
}
