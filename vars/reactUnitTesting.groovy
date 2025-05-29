def call() {
    echo "Running React unit tests with coverage at root level..."
    sh '''
        npm install
        npm install --save-dev @testing-library/react@12.1.5 @testing-library/jest-dom@5.16.5
        npm test -- --coverage --watchAll=false
    '''
}
