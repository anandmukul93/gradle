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

package org.gradle.internal.operations.dump;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.gradle.internal.UncheckedException;
import org.gradle.internal.progress.BuildOperationDescriptor;
import org.gradle.internal.progress.BuildOperationListener;
import org.gradle.internal.progress.OperationFinishEvent;
import org.gradle.internal.progress.OperationStartEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.gradle.internal.Cast.uncheckedCast;

public class DumpingBuildOperationListener implements BuildOperationListener {

    public static final String SYSPROP = "org.gradle.operations.dump";

    private final Map<Object, PendingBuildOperation> operations = new LinkedHashMap<Object, PendingBuildOperation>();

    @Override
    public void started(BuildOperationDescriptor buildOperation, OperationStartEvent startEvent) {
        operations.put(buildOperation.getId(), new PendingBuildOperation(buildOperation, startEvent));
    }

    @Override
    public void finished(BuildOperationDescriptor buildOperation, OperationFinishEvent finishEvent) {
        PendingBuildOperation pending = operations.get(buildOperation.getId());
        pending.endTime = finishEvent.getEndTime();
        pending.result = finishEvent.getResult();
        pending.failure = finishEvent.getFailure();
    }

    public void writeTo(String path) {
        try {
            File file = new File(path);
            file.getParentFile().mkdirs();
            if (file.isFile()) {
                file.delete();
            } else {
                file.createNewFile();
            }

            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
            try {
                outputStream.writeObject(operations);
            } finally {
                outputStream.close();
            }
        } catch (IOException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
    }

    public static Map<Object, CompleteBuildOperation> read(String path) {
        try {
            File file = new File(path);
            assert file.isFile();

            ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file));
            try {
                Map<Object, PendingBuildOperation> object = uncheckedCast(inputStream.readObject());
                Map<Object, CompleteBuildOperation> completeOperationMap = Maps.transformValues(object, new Function<PendingBuildOperation, CompleteBuildOperation>() {
                    @Override
                    public CompleteBuildOperation apply(PendingBuildOperation input) {
                        return input.toComplete();
                    }
                });
                return ImmutableMap.copyOf(completeOperationMap);
            } finally {
                inputStream.close();
            }
        } catch (IOException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        } catch (ClassNotFoundException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
    }

}
