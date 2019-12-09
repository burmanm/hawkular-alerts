package org.hawkular.alerts.engine.impl.ispn.model;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(includeClasses = { IspnActionPlugin.class }, schemaPackageName = "org.hawkular.alerts.engine.impl.ispn.model")
interface ModelContextInitializer extends SerializationContextInitializer {
}
