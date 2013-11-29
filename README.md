This script creates HTML report with the list of failed tests for given view in Jenkins.

Build the project and run executable JAR from "target" folder, like

java -jar target/jenkins-reporter-0.0.1-SNAPSHOT-standalone.jar "LR 2.7" "LR 2.7 Staging"

The app will generate two HTML files and will open them in your browser.

NB! It only shows failed jobs in the reports, which are not in running state.  