/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glowroot.plugin.cassandra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.glowroot.api.Message;
import org.glowroot.api.MessageSupplier;
import org.glowroot.plugin.cassandra.SessionAspect.BatchStatement;
import org.glowroot.plugin.cassandra.SessionAspect.BoundStatement;
import org.glowroot.plugin.cassandra.SessionAspect.PreparedStatement;
import org.glowroot.plugin.cassandra.SessionAspect.RegularStatement;
import org.glowroot.plugin.cassandra.SessionAspect.Statement;

class BatchQueryMessageSupplier extends MessageSupplier {

    private final List<String> queries;

    static BatchQueryMessageSupplier from(Collection<Statement> statements) {
        List<String> queries = new ArrayList<String>();
        String currQuery = null;
        int currCount = 0;
        for (Statement statement : statements) {
            String query;
            if (statement instanceof RegularStatement) {
                String qs = ((RegularStatement) statement).getQueryString();
                query = nullToEmpty(qs);
            } else if (statement instanceof BoundStatement) {
                PreparedStatement preparedStatement =
                        ((BoundStatement) statement).preparedStatement();
                String qs = preparedStatement == null ? "" : preparedStatement.getQueryString();
                query = nullToEmpty(qs);
            } else if (statement instanceof BatchStatement) {
                query = "[nested batch statement]";
            } else {
                query = "[unexpected statement type: " + statement.getClass().getName() + "]";
            }
            if (currQuery == null) {
                currQuery = query;
                currCount = 1;
            } else if (query != currQuery) {
                if (currCount == 1) {
                    queries.add(currQuery);
                } else {
                    queries.add(currCount + " x " + currQuery);
                }
                currQuery = query;
                currCount = 1;
            } else {
                currCount++;
            }
        }
        if (currQuery != null) {
            if (currCount == 1) {
                queries.add(currQuery);
            } else {
                queries.add(currCount + " x " + currQuery);
            }
        }
        return new BatchQueryMessageSupplier(queries);
    }

    public BatchQueryMessageSupplier(List<String> queries) {
        super();
        this.queries = queries;
    }

    @Override
    public Message get() {
        StringBuilder sb = new StringBuilder();
        sb.append("cql execution: ");
        for (int i = 0; i < queries.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(queries.get(i));
        }
        return Message.from(sb.toString());
    }

    private static String nullToEmpty(@Nullable String string) {
        return string == null ? "" : string;
    }
}