/*
 * Copyright 2014 the original author or authors.
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

package org.apache.maven.plugin.compiler;

import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.IOException;

interface Encoder {
    void writeSmallInt(int value) throws IOException;

    void writeIntSet(IntSet value) throws IOException;

    void writeNullableString(String value) throws IOException;

    void writeByte(byte value) throws IOException;

    void writeString(String value) throws IOException;
}
