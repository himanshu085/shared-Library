def call() {
    return this
}

def checkoutCode() {
    echo "Checking out Salary API Java repository..."
    checkout([$class: 'GitSCM',
        branches: [[name: '*/main']],
        userRemoteConfigs: [[
            url: 'https://github.com/Cloud-NInja-snaatak/salary-api.git',
            credentialsId: 'himanshu-git-credential'
        ]]
    ])
}

def compileCode() {
    echo "Compiling Java code using Maven..."
    sh 'mvn clean compile'
}

def runUnitTests() {
    echo "Running unit tests..."
    sh 'mvn clean test'
}

def generateCoverage() {
    echo "Generating code coverage report..."
    sh 'mvn jacoco:report'
}

def publishCoverage() {
    echo "Publishing code coverage report..."
    publishHTML(target: [
        reportDir: 'target/site/jacoco',
        reportFiles: 'index.html',
        reportName: 'Java Code Coverage Report'
    ])
}

def installZAP(String zapDir, String zapUrl) {
    echo "Installing OWASP ZAP..."
    sh """
        mkdir -p ${zapDir}
        wget -q ${zapUrl} -O zap.tar.gz
        tar -xzf zap.tar.gz -C ${zapDir} --strip-components=1
        rm zap.tar.gz
    """
}

def dependencyCheck(String zapDir) {
    echo "Running basic dependency check with ZAP..."
    sh 'java -version'
    sh "${zapDir}/zap.sh -version"
}

def runDASTScan(String zapDir, String targetUrl, String reportPath) {
    echo "Running OWASP ZAP DAST scan..."
    sh """
        ${zapDir}/zap.sh -cmd \\
            -port 9090 \\
            -quickurl ${targetUrl} \\
            -quickout ${reportPath} \\
            -quickprogress
    """
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

    def buildTime = new Date().format("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone('Asia/Kolkata'))
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
*Time (IST):* ${buildTime}
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
    <p><strong>Time (IST):</strong> ${buildTime}</p>
    <p><strong>Build URL:</strong> <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>
    <p><strong>Coverage Report:</strong> <a href="${coverageUrl}">${coverageUrl}</a></p>
    ${failureReason}
  </body>
</html>
"""

    slackSend(channel: slackChannel, color: colors[status], message: slackMessage)
    mail(to: emailRecipients, subject: subjects[status], body: emailBody, mimeType: 'text/html')
}
