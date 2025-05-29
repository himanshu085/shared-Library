def call() {
    dir('frontend') {
        // Run unit tests with coverage
        sh 'npm test -- --coverage --watchAll=false'
    }
}
