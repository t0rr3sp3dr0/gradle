/*
 * Copyright 2019 the original author or authors.
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

package org.gradle.process.internal.util;

import org.gradle.internal.os.OperatingSystem;

import java.util.List;

public class LongCommandLineDetectionUtil {
    // See http://msdn.microsoft.com/en-us/library/windows/desktop/ms682425(v=vs.85).aspx
    public static final int ARG_MAX_WINDOWS = 32767; // in chars
    public static final int ENVIRONMENT_VARIABLE_MAX_STRING_LENGTH = 32767; // in chars
    private static final String WINDOWS_LONG_COMMAND_EXCEPTION_MESSAGE = "The filename or extension is too long";

    private static int getMaxCommandLineLength() {
        if (OperatingSystem.current().isWindows()) {
            return ARG_MAX_WINDOWS;
        }
        return Integer.MAX_VALUE;
    }

    public static boolean hasCommandLineExceedMaxLength(String command, List<String> arguments) {
        int commandLineLength = command.length() + arguments.stream().map(String::length).reduce(Integer::sum).orElse(0) + arguments.size();
        return commandLineLength > getMaxCommandLineLength();
    }

    public static boolean hasCommandLineExceedMaxLengthException(Throwable failureCause) {
        Throwable cause = failureCause;
        do {
            if (cause.getMessage().contains(WINDOWS_LONG_COMMAND_EXCEPTION_MESSAGE)) {
                return true;
            }
        } while ((cause = cause.getCause()) != null);

        return false;
    }

    private static int getMaxEnvironmentVariableLength() {
        if (OperatingSystem.current().isWindows()) {
            return ENVIRONMENT_VARIABLE_MAX_STRING_LENGTH;
        }
        return Integer.MAX_VALUE;
    }

    public static boolean hasEnvironmentVariableExceedMaxLength(String value) {
        return value.length() > getMaxCommandLineLength();
    }
}
