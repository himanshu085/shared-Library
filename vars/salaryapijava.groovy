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
    def coverageUrl = "${env.BUILD_URL}Java_Code_Coverage_Report/"
    def message = """${icons[status]} ${priority} ${status}: ${results[priority][status]}
Job: '${env.JOB_NAME} [${env.BUILD_NUMBER}]' <${env.BUILD_URL}|Details>
Coverage Report: ${coverageUrl}"""

    slackSend(channel: slackChannel, color: colors[status], message: message)

    try {
        mail(to: emailRecipients, subject: subjects[status], body: "${message}\n\nCheck details here: ${env.BUILD_URL}")
    } catch (err) {
        echo "⚠️ Email sending failed: ${err.message}"
    }
}
