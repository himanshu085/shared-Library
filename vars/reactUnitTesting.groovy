def call() {
    dir('frontend') {
        // Install project dependencies (this includes react-scripts)
        sh 'npm install'

        // Run unit tests with coverage
        sh 'npm test -- --coverage --watchAll=false'
    }
}
