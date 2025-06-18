package org.snaatak.common

import java.text.SimpleDateFormat

class Notification implements Serializable {
    def steps

    Notification(steps) {
        this.steps = steps
    }

    def send(String status, String priority, String slackChannel, String emailRecipients) {
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
        def reportUrl = "${steps.env.BUILD_URL}React_Test_Report/"

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
📊 *Report:* <${reportUrl}|View Report>${status == "FAILURE" ? "\n❌ *Reason for Failure:* `${failureReason}`" : ""}
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
    <p><strong>Report:</strong> <a href="${reportUrl}">${reportUrl}</a></p>
    ${failureReason}
  </body>
</html>
"""

        steps.slackSend(channel: slackChannel, color: colors[status], message: slackMessage)
        steps.mail(to: emailRecipients, subject: subjects[status], body: emailBody, mimeType: 'text/html')
    }
}
