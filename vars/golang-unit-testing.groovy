def call() {
    // Your unit testing steps for golang here
    echo "Running golang unit tests"
    sh 'go test ./... -v'
}
