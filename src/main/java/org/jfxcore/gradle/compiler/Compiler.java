/*
 * Copyright (c) 2021, JFXcore
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jfxcore.gradle.compiler;

import org.gradle.api.logging.Logger;
import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

public class Compiler {

    public static final String COMPILER_NAME = "org.jfxcore.compiler.Compiler";
    private static final String LOGGER_NAME = "org.jfxcore.compiler.Logger";

    private final Object compilerInstance;
    private final Method parseFilesMethod;
    private final Method generateSourcesMethod;
    private final Method compileFilesMethod;

    public Compiler(Logger logger, Set<File> classpath, ClassLoader classLoader) throws Exception {
        Class<?> compilerLoggerClass = Class.forName(LOGGER_NAME, true, classLoader);

        Object compilerLogger = Proxy.newProxyInstance(
            compilerLoggerClass.getClassLoader(),
            new Class[] {compilerLoggerClass},
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args)
                        throws InvocationTargetException, IllegalAccessException {
                    switch (method.getName()) {
                        case "debug":
                            logger.debug((String)args[0]);
                            return null;

                        case "info":
                            logger.lifecycle((String)args[0]);
                            return null;

                        case "error":
                            logger.error((String)args[0]);
                            return null;
                    }

                    return method.invoke(proxy, args);
                }
            });

        compilerInstance = Class.forName(COMPILER_NAME, true, classLoader)
            .getConstructor(Set.class, compilerLoggerClass)
            .newInstance(classpath, compilerLogger);

        parseFilesMethod = compilerInstance.getClass().getMethod("parseFiles", File.class);
        generateSourcesMethod = compilerInstance.getClass().getMethod("generateSources", File.class);
        compileFilesMethod = compilerInstance.getClass().getMethod("compileFiles");
    }

    public void parseFiles(File sourceDir) throws Throwable {
        try {
            parseFilesMethod.invoke(compilerInstance, sourceDir);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    public void generateSources(File generatedSourcesDir) throws Throwable {
        try {
            generateSourcesMethod.invoke(compilerInstance, generatedSourcesDir);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    public void compileFiles() throws Throwable {
        try {
            compileFilesMethod.invoke(compilerInstance);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

}
