def call(Map config = [:]) {
    pipeline {
        agent any

        environment {
            PATH = "/usr/local/go/bin:${env.PATH}"
            GO111MODULE = 'on'
            PRIORITY = config.priority ?: 'P0'
            SLACK_CHANNEL = config.slackChannel ?: '#cloudninja'
            EMAIL_RECIPIENTS = config.emailRecipients ?: 'you@example.com'
        }

        stages {
            stage('Checkout') {
                steps {
                    git branch: config.branch ?: 'main', url: config.repo ?: 'https://github.com/OT-MICROSERVICES/employee-api.git'
                }
            }

            stage('Dependency Management') {
                steps {
                    dir('routes') {
                        sh 'go mod tidy'
                    }
                }
            }

            stage('Unit Tests') {
                steps {
                    dir('routes') {
                        sh 'go test -v ./...'
                    }
                }
            }

            stage('Code Coverage') {
                steps {
                    dir('routes') {
                        sh 'go test -coverprofile=coverage.out ./...'
                        sh 'go tool cover -html=coverage.out -o coverage.html'
                    }
                }
            }

            stage('Publish Coverage Report') {
                steps {
                    publishHTML(target: [
                        reportDir: 'routes',
                        reportFiles: 'coverage.html',
                        reportName: 'Go Test Coverage Report'
                    ])
                }
            }
        }

        post {
            success {
                notify('SUCCESS', env.PRIORITY, env.SLACK_CHANNEL, env.EMAIL_RECIPIENTS)
            }
            failure {
                notify('FAILURE', env.PRIORITY, env.SLACK_CHANNEL, env.EMAIL_RECIPIENTS)
            }
            always {
                archiveArtifacts artifacts: '**/coverage.out, **/coverage.html', fingerprint: true
                echo "Cleaning up workspace"
                deleteDir()
            }
        }
    }
}

// Embedded utility function inside same file
def notify(String status, String priority, String slackChannel, String emailRecipients) {
    def icons = [SUCCESS: '✅', FAILURE: '❌']
    def results = [
        P0: [SUCCESS: '🔥 Urgent job completed!', FAILURE: '🚨 Urgent job FAILED!'],
        P1: [SUCCESS: '⚠️ Important job succeeded!', FAILURE: '⚠️ Important job FAILED!'],
        P2: [SUCCESS: '📘 Routine job passed.', FAILURE: '📘 Routine job failed.']
    ]
    def colors = [SUCCESS: 'good', FAILURE: 'danger']
    def subjects = [
        SUCCESS: "${priority} SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
        FAILURE: "${priority} FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
    ]
    def message = "${icons[status]} ${results[priority][status]}\nJob: ${env.JOB_NAME} #${env.BUILD_NUMBER} <${env.BUILD_URL}|Details>"

    slackSend(channel: slackChannel, color: colors[status], message: message)
    mail(to: emailRecipients, subject: subjects[status], body: "${message}\n\nDetails: ${env.BUILD_URL}")
}
