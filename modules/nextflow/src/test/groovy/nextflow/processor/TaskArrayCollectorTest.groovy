/*
 * Copyright 2013-2023, Seqera Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nextflow.processor

import java.nio.file.Paths

import com.google.common.hash.HashCode
import nextflow.Session
import nextflow.executor.Executor
import nextflow.executor.TaskArrayExecutor
import nextflow.script.BaseScript
import nextflow.script.BodyDef
import nextflow.script.ProcessConfig
import spock.lang.Specification
import test.TestHelper
/**
 *
 * @author Ben Sherman <bentshermann@gmail.com>
 */
class TaskArrayCollectorTest extends Specification {

    static class DummyExecutor extends Executor implements TaskArrayExecutor {
        TaskMonitor createTaskMonitor() { null }
        TaskHandler createTaskHandler(TaskRun task) { null }

        String getArrayIndexName() { null }
        int getArrayIndexStart() { 0 }
        String getArrayTaskId(String jobId, int index) { null }
    }

    def 'should submit tasks as job arrays' () {
        given:
        def executor = Mock(DummyExecutor)
        def handler = Mock(TaskHandler)
        def taskArray = [:] as TaskArrayRun
        def collector = Spy(new TaskArrayCollector(null, executor, 5)) {
            createTaskArray(_) >> taskArray
        }
        and:
        def task = Mock(TaskRun) {
            getConfig() >> Mock(TaskConfig) {
                getAttempt() >> 1
            }
        }

        // collect tasks into job array
        when:
        collector.collect(task)
        collector.collect(task)
        collector.collect(task)
        collector.collect(task)
        then:
        0 * executor.submit(_)

        // submit job array when it is ready
        when:
        collector.collect(task)
        then:
        1 * executor.submit(taskArray)

        // submit partial job array when closed
        when:
        collector.collect(task)
        collector.collect(task)
        collector.close()
        then:
        1 * executor.submit(taskArray)

        // submit tasks directly once closed
        when:
        collector.collect(task)
        then:
        1 * executor.submit(task)
    }

    def 'should submit retried tasks directly' () {
        given:
        def executor = Mock(DummyExecutor)
        def collector = Spy(new TaskArrayCollector(null, executor, 5))
        and:
        def task = Mock(TaskRun) {
            getConfig() >> Mock(TaskConfig) {
                getAttempt() >> 2
            }
        }

        when:
        collector.collect(task)
        then:
        1 * executor.submit(task)
    }

    def 'should create task array' () {
        given:
        def exec = Mock(DummyExecutor) {
            getWorkDir() >> TestHelper.createInMemTempDir()
            getArrayIndexName() >> 'ARRAY_JOB_INDEX'
        }
        def config = Spy(ProcessConfig, constructorArgs: [Mock(BaseScript), 'PROC']) {
            createTaskConfig() >> Mock(TaskConfig)
            get('cpus') >> 4
            get('tag') >> 'foo'
        }
        def proc = Mock(TaskProcessor) {
            getConfig() >> config
            getExecutor() >> exec
            getName() >> 'PROC'
            getSession() >> Mock(Session)
            isSingleton() >> false
            getTaskBody() >> { new BodyDef(null, 'source') }
        }
        def collector = Spy(new TaskArrayCollector(proc, exec, 5))
        and:
        def task = Mock(TaskRun) {
            index >> 1
            getHash() >> HashCode.fromString('0123456789abcdef')
        }
        def handler = Mock(TaskHandler) {
            getTask() >> task
            getWorkDir() >> Paths.get('/work/foo')
            getLaunchCommand() >> ['bash', '-o', 'pipefail', '-c', 'bash /work/foo/.command.run 2>&1 | tee /work/foo/.command.log']
        }

        when:
        def taskArray = collector.createTaskArray([task, task, task])
        then:
        3 * exec.createTaskHandler(task) >> handler
        3 * handler.prepareLauncher()
        and:
        taskArray.name == 'PROC (1)'
        taskArray.config.cpus == 4
        taskArray.config.tag == null
        taskArray.processor == proc
        taskArray.script == '''
            array=( /work/foo /work/foo /work/foo )
            export task_dir=${array[ARRAY_JOB_INDEX]}
            bash -o pipefail -c 'bash ${task_dir}/.command.run 2>&1 | tee ${task_dir}/.command.log'
            '''.stripIndent().leftTrim()
        and:
        taskArray.getArraySize() == 3
        taskArray.getContainerConfig().getEnvWhitelist() == [ 'ARRAY_JOB_INDEX' ]
        taskArray.isContainerEnabled() == false
    }

}
