// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.nereids.trees.expressions.functions.scalar;

import org.apache.doris.catalog.FunctionSignature;
import org.apache.doris.nereids.exceptions.AnalysisException;
import org.apache.doris.nereids.parser.NereidsParser;
import org.apache.doris.nereids.trees.expressions.Expression;
import org.apache.doris.nereids.trees.expressions.functions.ExplicitlyCastableSignature;
import org.apache.doris.nereids.trees.expressions.functions.PropagateNullable;
import org.apache.doris.nereids.trees.expressions.literal.NullLiteral;
import org.apache.doris.nereids.trees.expressions.literal.StringLikeLiteral;
import org.apache.doris.nereids.trees.expressions.shape.BinaryExpression;
import org.apache.doris.nereids.trees.expressions.visitor.ExpressionVisitor;
import org.apache.doris.nereids.types.ArrayType;
import org.apache.doris.nereids.types.StringType;
import org.apache.doris.nereids.types.VarcharType;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * ScalarFunction 'tokenize'. This class is generated by GenerateFunction.
 */
public class Tokenize extends ScalarFunction
        implements BinaryExpression, ExplicitlyCastableSignature, PropagateNullable {

    public static final List<FunctionSignature> SIGNATURES = ImmutableList.of(
            FunctionSignature.ret(ArrayType.of(VarcharType.SYSTEM_DEFAULT))
                    .args(StringType.INSTANCE, StringType.INSTANCE)
    );

    /**
     * constructor with 2 arguments.
     */
    public Tokenize(Expression arg0, Expression arg1) {
        super("tokenize", arg0, arg1);
    }

    @Override
    public void checkLegalityBeforeTypeCoercion() {
        Expression rightChild = child(1);
        // tokenize(k7, null) could return NULL
        if (rightChild instanceof NullLiteral) {
            return;
        }
        if (!(rightChild instanceof StringLikeLiteral)) {
            throw new AnalysisException("tokenize second argument must be string literal");
        }
        String properties = ((StringLikeLiteral) child(1)).value;
        if (properties == null || properties.isEmpty()) {
            return;
        }
        try {
            new NereidsParser().parseProperties(((StringLikeLiteral) child(1)).value);
        } catch (Throwable e) {
            throw new AnalysisException("tokenize second argument must be properties format");
        }
    }

    /**
     * withChildren.
     */
    @Override
    public Tokenize withChildren(List<Expression> children) {
        Preconditions.checkArgument(children.size() == 2);
        return new Tokenize(children.get(0), children.get(1));
    }

    @Override
    public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitTokenize(this, context);
    }

    @Override
    public List<FunctionSignature> getSignatures() {
        return SIGNATURES;
    }
}
