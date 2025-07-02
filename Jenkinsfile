pipeline {
    agent any
    tools {
        // 配置 Maven 工具版本
        maven 'Maven 3.9.9'
    }
    stages {
        // 阶段 1: 拉取代码
        stage('Checkout') {
            steps {
                // 从 Git 仓库中拉取代码
                git 'https://github.com/saltedfish-lg/excel-api-auto-test.git'
            }
        }

        // 阶段 2: 编译与构建
        stage('Build') {
            steps {
                script {
                    // 使用 Maven 来清理和构建项目
                    sh 'mvn clean install'
                }
            }
        }

        // 阶段 3: 运行测试
        stage('Test') {
            steps {
                script {
                    // 执行测试，生成 TestNG 测试结果
                    sh 'mvn test'
                }
            }
        }

        // 阶段 4: 发布 TestNG 测试报告
        stage('Publish TestNG Report') {
            steps {
                // 发布 TestNG 测试结果报告
                publishTestNGResults(testResults: '**/target/test-classes/testng-results.xml')
            }
        }

        // 阶段 5: 发布 ExtentReports HTML 报告
        stage('Publish HTML Report') {
            steps {
                // 发布 ExtentReports HTML 测试报告
                publishHTML(target: [
                    reportName: 'Extent Report',
                    reportDir: 'target/extent-report',
                    reportFiles: 'index.html'
                ])
            }
        }
    }

    post {
        // 在构建后执行
        always {
            // 在每次构建完成后发送通知或执行其他操作
            echo '构建完成！'
        }

        success {
            // 如果构建成功，执行成功的操作
            echo '构建成功！'
        }

        failure {
            // 如果构建失败，执行失败的操作
            echo '构建失败！'
        }
    }
}
