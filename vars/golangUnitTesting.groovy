def call() {
    echo "Running GoLang unit testing pipeline stages..."
}

def checkoutCode() {
    echo "Checking out GoLang repository..."
    checkout([$class: 'GitSCM',
        branches: [[name: '*/main']],
        userRemoteConfigs: [[
            url: 'https://github.com/OT-MICROSERVICES/employee-api.git',
            credentialsId: 'himanshu-git-credential'
        ]]
    ])
}

def installGo(goVersion = '1.22.2') {
    echo "Installing Go ${goVersion} locally if not present..."
    def goRoot = "${env.WORKSPACE}/.go"
    def goBin = "${goRoot}/go/bin/go"
    def goUrl = "https://go.dev/dl/go${goVersion}.linux-amd64.tar.gz"

    def exists = sh(script: "${goBin} version", returnStatus: true) == 0

    if (!exists) {
        sh """
            rm -rf ${goRoot}
            mkdir -p ${goRoot}
            curl -sSL ${goUrl} -o go.tar.gz
            tar -C ${goRoot} -xzf go.tar.gz
            rm go.tar.gz
        """
    } else {
        sh "${goBin} version"
    }

    env.PATH = "${goRoot}/go/bin:${env.PATH}"
    env.GO111MODULE = 'on'
}

def tidyDependencies() {
    echo "Tidying Go modules..."
    dir('routes') {
        sh 'go mod tidy'
    }
}

def runTests() {
    echo "Running unit tests..."
    dir('routes') {
        sh 'go test -v ./...'
    }
}

def generateCoverage() {
    echo "Generating code coverage..."
    dir('routes') {
        sh '''
            go test -coverprofile=coverage.out ./...
            go tool cover -html=coverage.out -o coverage.html
        '''
    }
}

def publishCoverage() {
    echo "Publishing GoLang test coverage report..."
    publishHTML(target: [
        reportDir: 'routes',
        reportFiles: 'coverage.html',
        reportName: 'Go Test Coverage Report'
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
