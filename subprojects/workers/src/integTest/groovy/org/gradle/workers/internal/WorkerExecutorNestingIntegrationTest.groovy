/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.workers.internal

import org.gradle.integtests.fixtures.timeout.IntegrationTestTimeout
import org.gradle.internal.work.DefaultConditionalExecutionQueue
import org.gradle.workers.fixtures.WorkerExecutorFixture
import spock.lang.Unroll

import static org.gradle.workers.fixtures.WorkerExecutorFixture.ISOLATION_MODES

@IntegrationTestTimeout(120)
class WorkerExecutorNestingIntegrationTest extends AbstractWorkerExecutorIntegrationTest {
    def nestingParameterType = fixture.workParameterClass("NestingParameter", "org.gradle.test").withFields([
        "greeting": "Property<String>",
        "childSubmissions": "int"
    ])

    @Unroll
    def "workers with no isolation can spawn more work with #nestedIsolationMode"() {
        buildFile << """
            ${getWorkActionWithNesting("IsolationMode.NONE", nestedIsolationMode)}
            task runInWorker(type: NestingWorkerTask)
        """.stripIndent()

        when:
        succeeds("runInWorker")

        then:
        outputContains("Hello World")

        where:
        nestedIsolationMode << ISOLATION_MODES
    }

    def "workers with no isolation can wait on spawned work"() {
        buildFile << """
            ${getWorkActionWithNesting("IsolationMode.NONE", "IsolationMode.NONE")}
            task runInWorker(type: NestingWorkerTask) {
                waitForChildren = true 
            }
        """.stripIndent()

        when:
        succeeds("runInWorker")

        then:
        result.groupedOutput.task(':runInWorker').output.contains("Hello World")
    }

    def "workers with no isolation can spawn more than max workers items of work"() {
        def maxWorkers = 4
        buildFile << """
            ${getWorkActionWithNesting("IsolationMode.NONE", "IsolationMode.NONE")}
            task runInWorker(type: NestingWorkerTask) {
                submissions = ${maxWorkers * 2}
                childSubmissions = ${maxWorkers}
            }
        """.stripIndent()

        when:
        executer.withArguments("--max-workers=${maxWorkers}")
        succeeds("runInWorker")

        then:
        result.groupedOutput.task(':runInWorker').output.contains("Hello World")
    }

    def "workers with no isolation can spawn and wait for more than max workers items of work"() {
        def maxWorkers = 4
        buildFile << """
            ${getWorkActionWithNesting("IsolationMode.NONE", "IsolationMode.NONE")}
            task runInWorker(type: NestingWorkerTask) {
                waitForChildren = true 
                submissions = ${maxWorkers * 2}
                childSubmissions = ${maxWorkers}
            }
        """.stripIndent()

        when:
        executer.withArguments("--max-workers=${maxWorkers}")
        succeeds("runInWorker")

        then:
        result.groupedOutput.task(':runInWorker').output.contains("Hello World")
    }

    /*
     * This is not intended, but current behavior. We'll need to find a way to pass the service
     * registry across the classloader isolation barrier.
     */
    @Unroll
    def "workers with classpath isolation cannot spawn more work with #nestedIsolationMode"() {
        buildFile << """
            ${getWorkActionWithNesting("IsolationMode.CLASSLOADER", nestedIsolationMode)}
            task runInWorker(type: NestingWorkerTask)
        """.stripIndent()

        expect:
        fails("runInWorker")

        and:
        failure.assertHasCause("Could not create an instance of type FirstLevelExecution.")
        failure.assertHasCause("Unable to determine constructor argument #1: missing parameter of interface org.gradle.workers.WorkerExecutor, or no service of type interface org.gradle.workers.WorkerExecutor")

        where:
        nestedIsolationMode << ISOLATION_MODES
    }

    /*
     * Ideally this would be possible, but it would require coordination between workers and the daemon
     * to figure out who is allowed to schedule more work without violating the max-workers setting.
     */
    @Unroll
    def "workers with process isolation cannot spawn more work with #nestedIsolationMode"() {
        buildFile << """
            ${getWorkActionWithNesting("IsolationMode.PROCESS", nestedIsolationMode)}
            task runInWorker(type: NestingWorkerTask)
        """.stripIndent()

        expect:
        fails("runInWorker")

        and:
        failure.assertHasCause("Could not create an instance of type FirstLevelExecution.")
        failure.assertHasCause("Unable to determine constructor argument #1: missing parameter of interface org.gradle.workers.WorkerExecutor, or no service of type interface org.gradle.workers.WorkerExecutor")

        where:
        nestedIsolationMode << ISOLATION_MODES
    }

    def "does not leave more than max-workers threads running when work items submit more work"() {
        def maxWorkers = 4

        buildFile << """
            ${getWorkActionWithNesting("IsolationMode.NONE", "IsolationMode.NONE")}
            task runInWorker(type: NestingWorkerTask) {
                submissions = ${maxWorkers * 2}
                childSubmissions = ${maxWorkers * 10}

                doLast {
                    def timeout = System.currentTimeMillis() + (${DefaultConditionalExecutionQueue.KEEP_ALIVE_TIME_MS} * 3)
                    
                    // Let the keep-alive time on the thread pool expire
                    sleep(${DefaultConditionalExecutionQueue.KEEP_ALIVE_TIME_MS})

                    def executorThreads = getWorkerExecutorThreads()
                    while(System.currentTimeMillis() < timeout) {
                        if (executorThreads.size() <= ${maxWorkers}) {
                            break
                        }
                        sleep 100
                        executorThreads = getWorkerExecutorThreads()
                    }
                        
                    println "\\nWorker Executor threads:"
                    executorThreads.each { println it }
                    
                    // Ensure that we don't leave any threads lying around
                    assert executorThreads.size() <= ${maxWorkers}
                }
            }

            def getWorkerExecutorThreads() {
                def threadGroup = Thread.currentThread().threadGroup
                def threads = new Thread[threadGroup.activeCount()]
                threadGroup.enumerate(threads)                     
                return threads.findAll { it?.name?.startsWith("${WorkerExecutionQueueFactory.QUEUE_DISPLAY_NAME}") } 
            }
        """.stripIndent()

        when:
        executer.withArguments("--max-workers=${maxWorkers}")
        succeeds("runInWorker")

        then:
        result.groupedOutput.task(':runInWorker').output.contains("Hello World")
    }

    WorkerExecutorFixture.WorkActionClass getFirstLevelExecution(String nestedIsolationMode) {
        def workerClass = fixture.workActionClass("FirstLevelExecution", "org.gradle.test", nestingParameterType)
        workerClass.imports += ["org.gradle.workers.WorkerExecutor"]
        workerClass.extraFields = """
            WorkerExecutor executor
            
            ${fixture.workerMethodTranslation}
        """
        workerClass.constructorArgs = "WorkerExecutor executor"
        workerClass.constructorAction = "this.executor = executor"
        workerClass.action = """
            def theGreeting = parameters.greeting
            parameters.childSubmissions.times {
                executor."\${getWorkerMethod($nestedIsolationMode)}"().submit(SecondLevelExecution) {
                    greeting.set(theGreeting)
                }
            }
        """
        return workerClass
    }

    WorkerExecutorFixture.WorkActionClass getSecondLevelExecution() {
        def workerClass = fixture.workActionClass("SecondLevelExecution", "org.gradle.test", nestingParameterType)
        workerClass.action = """
            System.out.println(parameters.greeting.get())
        """
        return workerClass
    }

    String getWorkActionWithNesting(String isolationMode, String nestedIsolationMode) {
        getFirstLevelExecution(nestedIsolationMode).writeToBuildFile()
        secondLevelExecution.writeToBuildFile()
        return """
            class NestingWorkerTask extends DefaultTask {

                WorkerExecutor executor
                boolean waitForChildren = false
                int submissions = 1
                int childSubmissions = 1

                @Inject
                NestingWorkerTask(WorkerExecutor executor) {
                    this.executor = executor
                }

                @TaskAction
                public void runInWorker() {
                    submissions.times {
                        executor."\${getWorkerMethod($isolationMode)}"().submit(FirstLevelExecution) {
                            greeting = "Hello World"
                            childSubmissions = this.childSubmissions
                        }
                    }
                    if (waitForChildren) {
                        executor.await()
                    }
                }

                ${fixture.workerMethodTranslation}
            }
        """.stripIndent()
    }
}
