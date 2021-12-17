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

import java.io.File;

public class ExceptionHelper {

    private static final String CLASS_NAME = "org.jfxcore.compiler.diagnostic.MarkupException";

    private final Class<?> markupExceptionClass;

    public ExceptionHelper(ClassLoader classLoader) {
        try {
            markupExceptionClass = Class.forName(CLASS_NAME, true, classLoader);
        } catch (ClassNotFoundException ex) {
            String message = "Class not found: " + ex.getMessage();
            throw new RuntimeException(message, ex);
        }
    }

    public boolean isMarkupException(RuntimeException ex) {
        return markupExceptionClass.isInstance(ex);
    }

    public String format(RuntimeException ex) {
        try {
            File sourceFile = (File)ex.getClass().getMethod("getSourceFile").invoke(ex);
            String message = (String)ex.getClass().getMethod("getMessageWithSourceInfo").invoke(ex);
            Object sourceInfo = ex.getClass().getMethod("getSourceInfo").invoke(ex);
            Object location = sourceInfo.getClass().getMethod("getStart").invoke(sourceInfo);
            int line = (int)location.getClass().getMethod("getLine").invoke(location);

            return String.format("%s:%s: %s", sourceFile != null ? sourceFile.toString() : "<null>", line + 1, message);
        } catch (ReflectiveOperationException ex2) {
            sneakyThrow(ex2);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E)e;
    }

}
