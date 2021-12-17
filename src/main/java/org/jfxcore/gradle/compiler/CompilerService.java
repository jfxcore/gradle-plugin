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

import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.api.tasks.SourceSet;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public abstract class CompilerService implements BuildService<CompilerService.Params>, AutoCloseable {

    public interface Params extends BuildServiceParameters {
        Property<String> getCompilerJar();
        SetProperty<File> getCompileClasspath();
    }

    private final Set<File> compileClasspath;
    private final CompilerClassLoader classLoader;
    private final ExceptionHelper exceptionHelper;
    private final Map<SourceSet, Compiler> compilers = new IdentityHashMap<>();

    public CompilerService() throws MalformedURLException {
        compileClasspath = getParameters().getCompileClasspath().getOrElse(Collections.emptySet());
        String compilerJar = getParameters().getCompilerJar().getOrNull();
        List<URL> urls = new ArrayList<>();

        if (compilerJar != null && !compilerJar.isEmpty()) {
            urls.add(new URL("file", null, compilerJar));
        }

        urls.addAll(compileClasspath.stream().map(file -> {
            try {
                return new URL("file", null, file.getCanonicalPath());
            } catch (IOException e) {
                return null;
            }
        }).filter(Objects::nonNull).toList());

        classLoader = new CompilerClassLoader(urls.toArray(URL[]::new), getClass().getClassLoader());
        checkDependencies(classLoader);

        exceptionHelper = new ExceptionHelper(classLoader);
    }

    @Override
    public void close() throws Exception {
        classLoader.close();
    }

    public ExceptionHelper getExceptionHelper() {
        return exceptionHelper;
    }

    public Compiler newCompiler(SourceSet sourceSet, Logger logger) throws Exception {
        Compiler instance = new Compiler(logger, compileClasspath, classLoader);
        compilers.put(sourceSet, instance);
        return instance;
    }

    public Compiler getCompiler(SourceSet sourceSet) {
        Compiler instance = compilers.get(sourceSet);
        if (instance != null) {
            return instance;
        }

        throw new IllegalStateException("No compiler found for source set '" + sourceSet.getName() + "'");
    }

    private static void checkDependencies(ClassLoader classLoader) {
        try {
            Class.forName(Compiler.COMPILER_NAME, true, classLoader);
        } catch (ClassNotFoundException ex) {
            throw new GradleException("Compiler not found");
        }

        List<String> missingDeps = new ArrayList<>();

        try {
            Class.forName("javafx.beans.Observable", true, classLoader);
        } catch (ClassNotFoundException ex) {
            missingDeps.add("javafx.base");
        }

        try {
            Class.forName("javafx.geometry.Bounds", true, classLoader);
        } catch (ClassNotFoundException ex) {
            missingDeps.add("javafx.graphics");
        }

        if (!missingDeps.isEmpty()) {
            throw new GradleException("Missing module dependencies: " + String.join(", ", missingDeps));
        }
    }

}
