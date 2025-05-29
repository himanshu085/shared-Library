def call() {
    stage('Unit Tests') {
        dir('routes') {
            sh 'go test -v ./... -coverprofile=coverage.out'
        }
    }
}
