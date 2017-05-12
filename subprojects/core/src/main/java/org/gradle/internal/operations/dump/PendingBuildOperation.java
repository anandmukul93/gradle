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

import groovy.json.JsonOutput;
import groovy.json.JsonSlurper;
import org.gradle.api.execution.internal.TaskOperationDetails;
import org.gradle.api.internal.tasks.SnapshotTaskInputsOperationDetails;
import org.gradle.internal.progress.BuildOperationDescriptor;
import org.gradle.internal.progress.OperationStartEvent;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Map;

import static org.gradle.internal.Cast.uncheckedCast;

class PendingBuildOperation implements Externalizable {

    private Object id;
    private Object parentId;
    private String name;
    private String displayName;

    private long startTime;

    private Object details;
    private Class<?> detailsType;
    private String detailsJson;

    long endTime;

    Object result;
    private Class<?> resultType;
    private String resultJson;

    Throwable failure;

    @SuppressWarnings("unused")
    public PendingBuildOperation() {
    }

    PendingBuildOperation(BuildOperationDescriptor descriptor, OperationStartEvent startEvent) {
        this.id = descriptor.getId();
        this.parentId = descriptor.getParentId();
        this.name = descriptor.getName();
        this.displayName = descriptor.getDisplayName();
        this.details = descriptor.getDetails();
        this.startTime = startEvent.getStartTime();
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        id = in.readObject();
        parentId = in.readObject();
        name = in.readUTF();
        displayName = in.readUTF();
        startTime = in.readLong();
        endTime = in.readLong();
        detailsType = uncheckedCast(in.readObject());

        try {
            detailsJson = uncheckedCast(in.readObject());
        } catch (Exception e) {
            throw new RuntimeException("failed to deserialize " + detailsType.getName(), e);
        }

        resultType = uncheckedCast(in.readObject());

        try {
            resultJson = uncheckedCast(in.readObject());
        } catch (Exception e) {
            throw new RuntimeException("failed to deserialize " + detailsType.getName(), e);
        }

        failure = uncheckedCast(in.readObject());
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(id);
        out.writeObject(parentId);
        out.writeUTF(name);
        out.writeUTF(displayName);
        out.writeLong(startTime);
        out.writeLong(endTime);
        out.writeObject(details == null ? null : details.getClass());
        out.writeObject(serialize(details));
        out.writeObject(result == null ? null : result.getClass());
        out.writeObject(serialize(result));
        out.writeObject(failure);
    }

    CompleteBuildOperation toComplete() {
        return new CompleteBuildOperation(
            id,
            parentId,
            name,
            displayName,
            startTime,
            endTime,
            detailsType,
            deserialize(detailsJson),
            resultType,
            deserialize(resultJson),
            failure
        );
    }

    private static Map<String, ?> deserialize(String json) {
        if (json == null) {
            return null;
        }
        JsonSlurper jsonSlurper = new JsonSlurper();
        return uncheckedCast(jsonSlurper.parseText(json));
    }

    private static String serialize(Object object) {
        if (object == null) {
            return null;
        }

        if (object instanceof TaskOperationDetails) {
            TaskOperationDetails cast = (TaskOperationDetails) object;
            object = Collections.singletonMap("task", cast.getTask().getPath());
        } else if (object instanceof SnapshotTaskInputsOperationDetails) {
            SnapshotTaskInputsOperationDetails cast = (SnapshotTaskInputsOperationDetails) object;
            object = Collections.singletonMap("task", cast.getTask().getPath());
        }

        return JsonOutput.toJson(object);
    }

}
