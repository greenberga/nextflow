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
 *
 */

package nextflow.cli

import static nextflow.cli.PluginExecAware.*

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import groovy.transform.CompileStatic
import nextflow.App
import nextflow.exception.AbortOperationException
/**
 * Plugin manager command
 * 
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
@Parameters(commandDescription = "Execute plugin-specific commands")
class CmdPlugin extends CmdBase {

    @Override
    String getName() {
        return 'plugin'
    }

    @Parameter(hidden = true)
    List<String> args

    @Override
    void run() {
        if( !args )
            throw new AbortOperationException("Missing plugin command - usage: nextflow plugin install <pluginId,..>")
        // check for the plugins install
        if( args[0] == 'install' ) {
            if( args.size()!=2 )
                throw new AbortOperationException("Missing plugin install target - usage: nextflow plugin install <pluginId,..>")
            App.instance.pluginService.pullPlugins(args[1].tokenize(','))
        }
        // plugin run command
        else if( args[0].contains(CMD_SEP) ) {
            final head = args.pop()
            final items = head.tokenize(CMD_SEP)
            final target = items[0]
            final cmd = items[1] ? items[1..-1].join(CMD_SEP) : null

            // push back the command as the first item
            App.instance.pluginService.start(target)
            final wrapper = App.instance.pluginService.manager.getPlugin(target)
            if( !wrapper )
                throw new AbortOperationException("Cannot find target plugin: $target")
            final plugin = wrapper.getPlugin()
            if( plugin instanceof PluginExecAware ) {
                final ret = plugin.exec(getLauncher(), target, cmd, args)
                // use explicit exit to invoke the system shutdown hooks
                System.exit(ret)
            }
            else
                throw new AbortOperationException("Invalid target plugin: $target")
        }
        else {
            throw new AbortOperationException("Invalid plugin command: ${args[0]}")
        }
    }

}
