/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.alerts.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hawkular.alerts.api.model.condition.EventCondition;
import org.hawkular.alerts.api.model.event.Event;
import org.junit.Test;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public class EventConditionTest {

    @Test
    public void testTenantIdExpression() {

        EventCondition condition = new EventCondition("trigger-1", "tenantId == 'my-organization'");
        Event event1 = new Event();
        event1.setTenantId("my-organization");

        assertTrue(condition.match(event1));

        Event event2 = new Event();
        event2.setTenantId("my-organization2");

        assertFalse(condition.match(event2));

        condition.setExpression("tenantId starts 'my-organiz'");

        assertTrue(condition.match(event1));
        assertTrue(condition.match(event2));

        condition.setExpression("tenantId ends '2'");

        assertFalse(condition.match(event1));
        assertTrue(condition.match(event2));

        condition.setExpression("tenantId contains 'organization'");

        assertTrue(condition.match(event1));
        assertTrue(condition.match(event2));

        condition.setExpression("tenantId matches 'my-organization.*'");

        assertTrue(condition.match(event1));
        assertTrue(condition.match(event2));
    }

    
}
