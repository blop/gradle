/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.api.internal.artifacts.transform;

import org.gradle.api.artifacts.transform.ArtifactTransform;
import org.gradle.api.artifacts.transform.VariantTransformConfigurationException;
import org.gradle.api.internal.artifacts.VariantTransformRegistry;
import org.gradle.api.internal.attributes.AttributeContainerInternal;
import org.gradle.api.internal.attributes.ImmutableAttributes;
import org.gradle.api.internal.changedetection.state.isolation.Isolatable;
import org.gradle.api.internal.changedetection.state.isolation.IsolatableFactory;
import org.gradle.internal.classloader.ClassLoaderHierarchyHasher;
import org.gradle.internal.hash.Hasher;
import org.gradle.internal.hash.Hashing;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.model.internal.type.ModelType;

import java.util.Arrays;

public class DefaultTransformRegistration implements VariantTransformRegistry.Registration {

    private final ImmutableAttributes from;
    private final ImmutableAttributes to;
    private final ArtifactTransformationStep transformation;

    public static VariantTransformRegistry.Registration create(ImmutableAttributes from, ImmutableAttributes to, Class<? extends ArtifactTransform> implementation, Object[] params, IsolatableFactory isolatableFactory, ClassLoaderHierarchyHasher classLoaderHierarchyHasher, Instantiator instantiator, TransformerInvoker transformerInvoker) {
        Hasher hasher = Hashing.newHasher();
        hasher.putString(implementation.getName());
        hasher.putHash(classLoaderHierarchyHasher.getClassLoaderHash(implementation.getClassLoader()));

        // TODO - should snapshot later?
        Isolatable<Object[]> paramsSnapshot;
        try {
            paramsSnapshot = isolatableFactory.isolate(params);
        } catch (Exception e) {
            throw new VariantTransformConfigurationException(String.format("Could not snapshot configuration values for transform %s: %s", ModelType.of(implementation).getDisplayName(), Arrays.asList(params)), e);
        }

        paramsSnapshot.appendToHasher(hasher);

        Transformer transformer = new DefaultTransformer(implementation, paramsSnapshot, hasher.hash(), instantiator);
        return new DefaultTransformRegistration(from, to, new ArtifactTransformationStep(transformer, transformerInvoker));
    }

    public DefaultTransformRegistration(ImmutableAttributes from, ImmutableAttributes to, ArtifactTransformationStep transformation) {
        this.from = from;
        this.to = to;
        this.transformation = transformation;
    }

    @Override
    public AttributeContainerInternal getFrom() {
        return from;
    }

    @Override
    public AttributeContainerInternal getTo() {
        return to;
    }

    @Override
    public ArtifactTransformation getTransformation() {
        return transformation;
    }
}
