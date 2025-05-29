def call() {
    dir('frontend') {
        // Install dependencies including testing libraries
        sh '''
            npm install
            npm install --save-dev @testing-library/react@12.1.5 @testing-library/jest-dom@5.16.5
        '''

        // Run unit tests without watch mode
        sh 'npm test -- --watchAll=false'

        // Run tests again to generate coverage report without watch mode
        sh 'npm test -- --coverage --watchAll=false'
    }
}
