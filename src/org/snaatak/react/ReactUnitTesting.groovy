package org.snaatak.react

import java.text.SimpleDateFormat

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
        steps.stage('Generate Coverage Report') {
            steps.dir('frontend') {
                steps.sh 'npm test -- --coverage --coverageDirectory=coverage --watchAll=false || true'
            }
        }
    }

    def publishCoverage() {
        steps.stage('Publish Coverage Report') {
            steps.publishHTML(target: [
                reportDir: 'frontend/coverage/lcov-report',
                reportFiles: 'index.html',
                reportName: 'React Test Coverage Report'
            ])
        }
    }

    def notify(String status, String priority, String slackChannel, String emailRecipients) {
        def icons = [SUCCESS: '🟢', FAILURE: '🔴']
        def results = [
            P0: [SUCCESS: 'Urgent job completed successfully! ✅', FAILURE: 'Urgent job FAILED! 🚨'],
            P1: [SUCCESS: 'Important job completed successfully! ✅', FAILURE: 'Important job FAILED! ⚠️'],
            P2: [SUCCESS: 'Standard job completed! ✅', FAILURE: 'Standard job FAILED. ❗']
        ]
        def colors = [SUCCESS: 'good', FAILURE: 'danger']
        def subjects = [
            SUCCESS: "${priority} SUCCESS: Jenkins Job '${steps.env.JOB_NAME} [${steps.env.BUILD_NUMBER}]'",
            FAILURE: "${priority} FAILURE: Jenkins Job '${steps.env.JOB_NAME} [${steps.env.BUILD_NUMBER}]'"
        ]

        def buildTime = new Date().format("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone('Asia/Kolkata'))
        def triggeredBy = steps.currentBuild.getBuildCauses().find { it.userId }?.userName ?: "Automated/Unknown"
        def coverageUrl = "${steps.env.BUILD_URL}React_Test_Coverage_Report/"

        def failureReason = ""
        if (status == "FAILURE") {
            def logLines = steps.currentBuild.rawBuild.getLog(100)
            def errorLine = logLines.find { it.contains("Exception") || it.contains("ERROR") || it.contains("FAILURE") }
            failureReason = errorLine ? "<p><strong>Reason for Failure:</strong> ${errorLine.trim()}</p>" : "<p><strong>Reason for Failure:</strong> Not found in last 100 log lines.</p>"
        }

        def slackMessage = """
${icons[status]} *${priority} ${status}*
*Status:* ${results[priority][status]}
*Job:* `${steps.env.JOB_NAME}`
*Build Number:* #${steps.env.BUILD_NUMBER}
*Triggered By:* ${triggeredBy}
*Time (IST):* ${buildTime}
🔗 *Build URL:* <${steps.env.BUILD_URL}|Click to view build>
📊 *Coverage Report:* <${coverageUrl}|View Coverage>${status == "FAILURE" ? "\n❌ *Reason for Failure:* `${failureReason}`" : ""}
"""

        def emailBody = """
<html>
  <body>
    <p><strong>${icons[status]} ${priority} ${status}</strong></p>
    <p><strong>Status:</strong> ${results[priority][status]}</p>
    <p><strong>Job:</strong> ${steps.env.JOB_NAME}</p>
    <p><strong>Build Number:</strong> #${steps.env.BUILD_NUMBER}</p>
    <p><strong>Triggered By:</strong> ${triggeredBy}</p>
    <p><strong>Time (IST):</strong> ${buildTime}</p>
    <p><strong>Build URL:</strong> <a href="${steps.env.BUILD_URL}">${steps.env.BUILD_URL}</a></p>
    <p><strong>Coverage Report:</strong> <a href="${coverageUrl}">${coverageUrl}</a></p>
    ${failureReason}
  </body>
</html>
"""
        steps.slackSend(channel: slackChannel, color: colors[status], message: slackMessage)
        steps.mail(to: emailRecipients, subject: subjects[status], body: emailBody, mimeType: 'text/html')
    }

    def executePipeline(Map config = [:]) {
        def branch = config.get('branch', 'main')
        def repoUrl = config.get('repoUrl', 'https://github.com/Cloud-NInja-snaatak/frontend-api.git')
        def credentialsId = config.get('credentialsId', 'himanshu-git-credential')

        checkoutCode(branch, repoUrl, credentialsId)
        installDependencies()
        runUnitTests()
        generateCoverage()
        publishCoverage()
    }
}
