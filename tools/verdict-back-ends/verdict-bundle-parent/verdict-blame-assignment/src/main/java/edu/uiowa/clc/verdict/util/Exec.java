/* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   Copyright (c) 2019-2020, General Electric Company.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.

    @author: Paul Meng
    @author: M. Fareed Arif
*/

package edu.uiowa.clc.verdict.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;

public class Exec {

    public static int run_kind2(
            File lustureFile, File kind2ResultFile, boolean emptySelection, boolean meritAssignment)
            throws IOException {

        //        InputStream resultStream = null;
        final String KIND2 = "kind2";

        OSCheck.OSType ostype = OSCheck.getOperatingSystemType();

        Path env_path = Paths.get(System.getenv("PATH"));
        String[] env = env_path.toString().split(":");

        // String value = System.getenv("MY_ENV_VAR")

        String dir_path = System.getProperty("user.dir");
        File user_dir = new File(dir_path);

        switch (ostype) {
            case Windows:
                LOGGY.info("OS:Windows");
                //                env_command = "psexec -i cmd /c start ";

                break;
            case MacOS:
                LOGGY.info("OS:MacOS");
                //                env_command = "/bin/sh -c ";
                break;
            case Linux:
                LOGGY.info("OS:Linux");
                //                env_command = "/bin/sh -c ";
                break;
            case Other:
                // Docker
                break;
        }

        LOGGY.info("Searching Kind2 on Path:" + user_dir);

        List<String> cmd_arg = new ArrayList<String>();

        File kind2 = new File(user_dir, KIND2);

        cmd_arg.add(kind2.getAbsolutePath());

        if (emptySelection || meritAssignment) {
            cmd_arg.add("--ivc");
            cmd_arg.add(Boolean.toString(meritAssignment));
            cmd_arg.add("--ivc_category");
            cmd_arg.add("contracts");
        } else {
            cmd_arg.add("--enable");
            cmd_arg.add("MCS");
            cmd_arg.add("--print_mcs_legacy");
            cmd_arg.add("true");
        }

        cmd_arg.add(lustureFile.getAbsolutePath());

        cmd_arg.add("-xml");

        String cmd = cmd_arg.toString();

        LOGGY.info("Executing Command: " + cmd);

        FileOutputStream fStream = new FileOutputStream(kind2ResultFile);

        String[] cmd_args = cmd_arg.toArray(new String[cmd_arg.size()]);

        //
        //        int exitCode = exec(cmd_args, env, user_dir, fStream);
        // -- Mute the environment variable.
        int exitCode = exec2(cmd_args, null, fStream);

        return exitCode;
    }

    public static int exec2(String[] cmd_args, String[] envp, FileOutputStream stream)
            throws IOException {

        ExecuteStreamHandler streamHandler = new PumpStreamHandler(stream);

        Execute executor = new Execute(streamHandler);

        executor.setCommandline(cmd_args);
        //        executor.setWorkingDirectory(dir);
        executor.setEnvironment(envp);

        int resultCode = executor.execute();

        return resultCode;
    }

    public static int exec(String[] cmd_args, String[] envp, File dir, FileOutputStream output)
            throws IOException {

        Process process = Runtime.getRuntime().exec(cmd_args, envp, dir);
        int exit_code = 0;

        try {

            String input;

            InputStream resultStream = process.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(resultStream));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));

            while ((input = reader.readLine()) != null) {
                writer.write(input);
            }

            exit_code = process.waitFor();

            reader.close();
            writer.close();

        } catch (InterruptedException e) {
            e.printStackTrace();
            process.destroy();
        }

        return exit_code;
    }
}
