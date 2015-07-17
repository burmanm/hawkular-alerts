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
package org.hawkular.alerts.bus.sender;

import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

import javax.jms.TopicConnectionFactory;
import javax.jms.JMSException;
import javax.naming.InitialContext;

import org.hawkular.alerts.api.json.JsonUtil;
import org.hawkular.alerts.api.model.action.Action;
import org.hawkular.alerts.api.services.DefinitionsService;
import org.hawkular.alerts.api.services.ActionListener;
import org.hawkular.alerts.bus.log.MsgLogger;
import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.MessageId;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.producer.ProducerConnectionContext;
import org.hawkular.actions.api.model.ActionMessage;
import org.jboss.logging.Logger;



/**
 * An implementation of {@link org.hawkular.alerts.api.services.ActionListener} that will send listener
 * messages through the bus.
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public class ActionSender implements ActionListener {
    private final MsgLogger msgLogger = MsgLogger.LOGGER;
    private final Logger log = Logger.getLogger(ActionSender.class);
    private static final String CONNECTION_FACTORY = "java:/HawkularBusConnectionFactory";
    private static final String ACTIONS_TOPIC = "HawkularAlertsActionsTopic";
    private static final String DEFINITIONS_SERVICE =
            "java:app/hawkular-alerts-rest/CassDefinitionsServiceImpl";

    private TopicConnectionFactory conFactory;
    private ConnectionContextFactory ccf;
    private ProducerConnectionContext pcc;
    InitialContext ctx;

    DefinitionsService definitions;

    public ActionSender() {
    }

    @Override
    public void process(Action action) {
        try {
            init();
            if (pcc == null) {
                msgLogger.warnCannotConnectToBus();
                return;
            }
            String alert = action.getAlert() != null ? JsonUtil.toJson(action.getAlert()) : null;
            ActionMessage nMsg = new ActionMessage(action.getTenantId(), action.getActionPlugin(),
                    action.getActionId(), action.getMessage(), alert);
            if (definitions != null) {
                Map<String, String> properties = definitions.getAction(action.getTenantId(),
                        action.getActionPlugin(), action.getActionId());
                Map<String, String> defaultProperties = definitions.getDefaultActionPlugin(action.getActionPlugin());
                nMsg.setProperties(properties);
                nMsg.setDefaultProperties(defaultProperties);
                MessageId mid = new MessageProcessor().send(pcc, nMsg, actionPluginFilter(action.getActionPlugin()));
                log.debug("Sent action message [" + mid.getId() + "] to the bus");
            } else {
                msgLogger.warnCannotAccessToDefinitionsService();
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            msgLogger.errorProcessingAction(e.getMessage());
        } finally {
            if (pcc != null) {
                try {
                    pcc.close();
                    pcc = null;
                } catch (IOException ignored) { }
            }
            if (ccf != null) {
                try {
                    ccf.close();
                    ccf = null;
                } catch (JMSException ignored) { }
            }
        }
    }

    private void init() throws Exception {
        if (ctx == null) {
            ctx = new InitialContext();
        }
        if (conFactory == null) {
            conFactory = (TopicConnectionFactory) ctx.lookup(CONNECTION_FACTORY);
        }
        if (ccf == null) {
            ccf = new ConnectionContextFactory(conFactory);
        }
        if (pcc == null) {
            pcc = ccf.createProducerConnectionContext(new Endpoint(Endpoint.Type.TOPIC, ACTIONS_TOPIC));
        }
        if (definitions == null) {
            definitions = (DefinitionsService) ctx.lookup(DEFINITIONS_SERVICE);
        }
    }

    private static Map<String, String> actionPluginFilter(String actionPlugin) {
        Map<String, String> map = new HashMap<>(1);
        map.put("actionPlugin", actionPlugin);
        return map;
    }
}
