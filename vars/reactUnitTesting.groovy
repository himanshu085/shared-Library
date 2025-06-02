def call() {
    echo "Running React unit testing pipeline stages..."
}

def checkoutCode() {
    echo "Checking out React repository..."
    checkout([$class: 'GitSCM',
        branches: [[name: '*/main']],
        userRemoteConfigs: [[
            url: 'https://github.com/Cloud-NInja-snaatak/frontend-api.git',
            credentialsId: 'himanshu-git-credential'
        ]]
    ])
}

def installDependencies() {
    echo "Installing React dependencies..."
    dir('frontend') {
        sh '''
            npm install
            npm install --save-dev @testing-library/react@12.1.5 @testing-library/jest-dom@5.16.5
        '''
    }
}

def runTests() {
    echo "Running unit tests..."
    dir('frontend') {
        sh 'npm test -- --watchAll=false > test.log 2>&1 || true'
    }
}

def generateCoverage() {
    echo "Generating coverage report..."
    dir('frontend') {
        sh 'npm test -- --coverage --coverageDirectory=coverage --watchAll=false || true'
    }
}

def publishCoverage() {
    echo "Publishing test coverage report..."
    publishHTML(target: [
        reportDir: 'coverage/lcov-report',
        reportFiles: 'index.html',
        reportName: 'React Test Coverage Report'
    ])
}

def notify(status, priority, slackChannel, emailRecipients) {
    def icons = [SUCCESS: '🟢', FAILURE: '🔴']
    def results = [
        P0: [SUCCESS: 'Urgent job completed successfully! ✅', FAILURE: 'Urgent job FAILED! 🚨'],
        P1: [SUCCESS: 'Important job completed successfully! ✅', FAILURE: 'Important job FAILED! ⚠️'],
        P2: [SUCCESS: 'Standard job completed! ✅', FAILURE: 'Standard job FAILED. ❗']
    ]
    def colors = [SUCCESS: 'good', FAILURE: 'danger']
    def subjects = [
        SUCCESS: "${priority} SUCCESS: Jenkins Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
        FAILURE: "${priority} FAILURE: Jenkins Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
    ]

    def buildTime = new Date().format("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone('UTC'))
    def triggeredBy = currentBuild.getBuildCauses().find { it.userId }?.userName ?: "Automated/Unknown"
    def coverageUrl = "${env.BUILD_URL}React_Test_Coverage_Report/"

    def failureReason = ""
    if (status == "FAILURE") {
        def logLines = currentBuild.rawBuild.getLog(100)
        def errorLine = logLines.find { it.contains("Exception") || it.contains("ERROR") || it.contains("FAILURE") }
        failureReason = errorLine ? "<p><strong>Reason for Failure:</strong> ${errorLine.trim()}</p>" : "<p><strong>Reason for Failure:</strong> Not found in last 100 log lines.</p>"
    }

    def slackMessage = """
${icons[status]} *${priority} ${status}*
*Status:* ${results[priority][status]}
*Job:* `${env.JOB_NAME}`
*Build Number:* #${env.BUILD_NUMBER}
*Triggered By:* ${triggeredBy}
*Time (UTC):* ${buildTime}
🔗 *Build URL:* <${env.BUILD_URL}|Click to view build>
📊 *Coverage Report:* <${coverageUrl}|View Coverage>${status == "FAILURE" ? "\n❌ *Reason for Failure:* `${failureReason}`" : ""}
"""

    def emailBody = """
<html>
  <body>
    <p><strong>${icons[status]} ${priority} ${status}</strong></p>
    <p><strong>Status:</strong> ${results[priority][status]}</p>
    <p><strong>Job:</strong> ${env.JOB_NAME}</p>
    <p><strong>Build Number:</strong> #${env.BUILD_NUMBER}</p>
    <p><strong>Triggered By:</strong> ${triggeredBy}</p>
    <p><strong>Time (UTC):</strong> ${buildTime}</p>
    <p><strong>Build URL:</strong> <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>
    <p><strong>Coverage Report:</strong> <a href="${coverageUrl}">${coverageUrl}</a></p>
    ${failureReason}
  </body>
</html>
"""

    slackSend(channel: slackChannel, color: colors[status], message: slackMessage)
    mail(to: emailRecipients, subject: subjects[status], body: emailBody, mimeType: 'text/html')
}
