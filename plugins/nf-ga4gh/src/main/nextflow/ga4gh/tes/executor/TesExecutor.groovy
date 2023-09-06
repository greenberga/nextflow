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

package nextflow.ga4gh.tes.executor

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.util.logging.Slf4j
import nextflow.exception.AbortOperationException
import nextflow.executor.Executor
import nextflow.extension.FilesEx
import nextflow.ga4gh.tes.client.ApiClient
import nextflow.ga4gh.tes.client.api.TaskServiceApi
import nextflow.ga4gh.tes.client.auth.ApiKeyAuth
import nextflow.ga4gh.tes.client.auth.Authentication
import nextflow.ga4gh.tes.client.auth.HttpBasicAuth
import nextflow.ga4gh.tes.client.auth.OAuth
import nextflow.processor.TaskHandler
import nextflow.processor.TaskMonitor
import nextflow.processor.TaskPollingMonitor
import nextflow.processor.TaskRun
import nextflow.util.Duration
import nextflow.util.ServiceName
import org.pf4j.ExtensionPoint

import java.nio.file.Path

/**
 * Experimental TES executor
 *
 * See https://github.com/ga4gh/task-execution-schemas/
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@CompileStatic
@ServiceName('tes')
class TesExecutor extends Executor implements ExtensionPoint {

    private TaskServiceApi client

    /**
     * A path accessible to TES where executable scripts need to be uploaded
     */
    private Path remoteBinDir = null

    @Override
    protected void register() {
        super.register()
        uploadBinDir()

        client = new TaskServiceApi( new ApiClient(basePath: getEndpoint(), authentications: getAuthentications()) )
    }

    protected String getDisplayName() {
        return "$name [${getEndpoint()}]"
    }

    TaskServiceApi getClient() {
        client
    }

    @PackageScope
    Path getRemoteBinDir() {
        remoteBinDir
    }

    protected void uploadBinDir() {
        /*
         * upload local binaries
         */
        if( session.binDir && !session.binDir.empty() && !session.disableRemoteBinDir ) {
            def tempBin = getTempDir()
            log.info "Uploading local `bin` scripts folder to ${tempBin.toUriString()}/bin"
            remoteBinDir = FilesEx.copyTo(session.binDir, tempBin)
        }
    }

    protected String getEndpoint() {
        def result = session.getConfigAttribute('executor.tes.endpoint', null)
        if( result )
            log.warn 'Config option `executor.tes.endpoint` is deprecated, use `tes.endpoint` instead'
        else
            result = session.config.navigate('tes.endpoint', 'http://localhost:8000')

        log.debug "[TES] endpoint=$result"
        return result
    }

    protected Map<String, Authentication> getAuthentications() {
        def result = [:] as Map<String, Authentication>

        // basic
        def username = session.config.navigate('tes.basicUsername')
        def password = session.config.navigate('tes.basicPassword')
        if( username && password )
            result['basic'] = new HttpBasicAuth(username: username, password: password)

        // API key
        def apiKeyParamMode = session.config.navigate('tes.apiKeyParamMode', 'query') as String
        def apiKeyParamName = session.config.navigate('tes.apiKeyParamName') as String
        def apiKey = session.config.navigate('tes.apiKey') as String
        if( apiKeyParamName && apiKey ) {
            def auth = new ApiKeyAuth(apiKeyParamMode, apiKeyParamName)
            auth.setApiKey(apiKey)
            result['apikey'] = auth
        }

        // OAuth
        def oauthToken = session.config.navigate('tes.oauthToken')
        if( oauthToken )
            result['oauth'] = new OAuth(accessToken: oauthToken)

        return result
    }

    /**
     * @return {@code true} whenever the containerization is managed by the executor itself
     */
    boolean isContainerNative() {
        return true
    }

    /**
     * Create a a queue holder for this executor
     *
     * @return
     */
    TaskMonitor createTaskMonitor() {
        return TaskPollingMonitor.create(session, name, 100, Duration.of('1 sec'))
    }


    /*
     * Prepare and launch the task in the underlying execution platform
     */
    @Override
    TaskHandler createTaskHandler(TaskRun task) {
        assert task
        assert task.workDir
        log.debug "[TES] Launching process > ${task.name} -- work folder: ${task.workDir}"
        new TesTaskHandler(task, this)
    }
}



